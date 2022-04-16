package com.gitbitex.order;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.kafka.PendingOffsetManager;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.log.OrderDoneLog.DoneReason;
import com.gitbitex.order.command.FillOrderCommand;
import com.gitbitex.order.command.OrderCommand;
import com.gitbitex.order.command.SaveOrderCommand;
import com.gitbitex.order.command.UpdateOrderStatusCommand;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderStatus;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public class OrderCommandShardingThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private final PendingOffsetManager pendingOffsetManager = new PendingOffsetManager();


    public OrderCommandShardingThread(String productId, KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                                      KafkaMessageProducer messageProducer, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                pendingOffsetManager.commit(consumer);

                for (TopicPartition partition : partitions) {
                    logger.info("partition revoked: {}", partition.toString());
                    pendingOffsetManager.remove(partition);
                }
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                for (TopicPartition partition : partitions) {
                    logger.info("partition assigned: {}", partition.toString());
                    pendingOffsetManager.put(partition);
                }
            }
        });
    }

    @Override
    protected void processRecords(ConsumerRecords<String, OrderBookLog> records) {

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());

            pendingOffsetManager.retainOffset(partition, record.offset());
            OrderBookLog log = record.value();
            if (log instanceof OrderReceivedLog) {
                on((OrderReceivedLog) log, partition, record.offset());
            } else if (log instanceof OrderOpenLog) {
                on((OrderOpenLog) log, partition, record.offset());
            } else if (log instanceof OrderMatchLog) {
                on((OrderMatchLog) log, partition, record.offset());
            } else if (log instanceof OrderDoneLog) {
                on((OrderDoneLog) log, partition, record.offset());
            } else {
                throw new RuntimeException("unknown log");
            }
            pendingOffsetManager.releaseOffset(partition, record.offset());
        }

        pendingOffsetManager.commit(consumer);
    }

    public void on(OrderReceivedLog log, TopicPartition partition, long offset) {
        SaveOrderCommand saveOrderCommand = new SaveOrderCommand();
        saveOrderCommand.setOrderId(log.getOrder().getOrderId());
        saveOrderCommand.setOrder(log.getOrder());
        sendCommand(saveOrderCommand, partition, offset);
    }

    public void on(OrderOpenLog log, TopicPartition partition, long offset) {
        UpdateOrderStatusCommand updateOrderStatusCommand = new UpdateOrderStatusCommand();
        updateOrderStatusCommand.setOrderId(log.getOrderId());
        updateOrderStatusCommand.setOrderStatus(Order.OrderStatus.OPEN);
        sendCommand(updateOrderStatusCommand, partition, offset);
    }

    public void on(OrderMatchLog log, TopicPartition partition, long offset) {
        FillOrderCommand fillTakerOrderCommand = new FillOrderCommand();
        fillTakerOrderCommand.setOrderId(log.getTakerOrderId());
        fillTakerOrderCommand.setSide(log.getSide().opposite());
        fillTakerOrderCommand.setProductId(log.getProductId());
        fillTakerOrderCommand.setSize(log.getSize());
        fillTakerOrderCommand.setPrice(log.getPrice());
        fillTakerOrderCommand.setFunds(log.getFunds());
        fillTakerOrderCommand.setTradeId(log.getTradeId());
        sendCommand(fillTakerOrderCommand, partition, offset);

        FillOrderCommand fillMakerOrderCommand = new FillOrderCommand();
        fillMakerOrderCommand.setOrderId(log.getMakerOrderId());
        fillMakerOrderCommand.setSide(log.getSide());
        fillMakerOrderCommand.setProductId(log.getProductId());
        fillMakerOrderCommand.setSize(log.getSize());
        fillMakerOrderCommand.setPrice(log.getPrice());
        fillMakerOrderCommand.setFunds(log.getFunds());
        fillMakerOrderCommand.setTradeId(log.getTradeId());
        sendCommand(fillMakerOrderCommand, partition, offset);
    }

    public void on(OrderDoneLog log, TopicPartition partition, long offset) {
        UpdateOrderStatusCommand updateOrderStatusCommand = new UpdateOrderStatusCommand();
        updateOrderStatusCommand.setOrderId(log.getOrderId());
        updateOrderStatusCommand.setOrderStatus(
                log.getDoneReason() == DoneReason.FILLED ? OrderStatus.FILLED : Order.OrderStatus.CANCELLED);
        updateOrderStatusCommand.setDoneReason(log.getDoneReason());
        sendCommand(updateOrderStatusCommand, partition, offset);
    }

    private void sendCommand(OrderCommand command, TopicPartition partition, long offset) {
        pendingOffsetManager.retainOffset(partition, offset);
        messageProducer.sendOrderCommand(command, (recordMetadata, e) -> {
            if (e != null) {
                logger.error("send order command error: {}", e.getMessage(), e);
                this.shutdown();
                return;
            }
            pendingOffsetManager.releaseOffset(partition, offset);
        });
    }
}
