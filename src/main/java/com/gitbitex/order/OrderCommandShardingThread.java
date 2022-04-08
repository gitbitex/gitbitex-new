package com.gitbitex.order;

import java.util.Collections;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderBookLogDispatcher;
import com.gitbitex.matchingengine.log.OrderBookLogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.order.entity.Order;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.command.FillOrderCommand;
import com.gitbitex.order.command.SaveOrderCommand;
import com.gitbitex.order.command.UpdateOrderStatusCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class OrderCommandShardingThread extends KafkaConsumerThread<String, OrderBookLog>
    implements OrderBookLogHandler {
    private final String productId;
    private final KafkaMessageProducer messageProducer;
    private final OrderBookLogDispatcher messageDispatcher;
    private final AppProperties appProperties;

    public OrderCommandShardingThread(String productId, KafkaConsumer<String, OrderBookLog> kafkaConsumer,
        KafkaMessageProducer messageProducer, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.messageDispatcher = new OrderBookLogDispatcher(this);
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookLog> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()));
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderBookLog> consumer,
        ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            messageDispatcher.dispatch(record.value());
        }
        consumer.commitSync();
    }

    @Override
    public void on(OrderReceivedLog log) {
        SaveOrderCommand saveOrderCommand = new SaveOrderCommand();
        saveOrderCommand.setOrderId(log.getOrder().getOrderId());
        saveOrderCommand.setOrder(log.getOrder());
        messageProducer.sendToOrderProcessor(saveOrderCommand);
    }

    @Override
    public void on(OrderOpenLog log) {
        UpdateOrderStatusCommand updateOrderStatusCommand = new UpdateOrderStatusCommand();
        updateOrderStatusCommand.setOrderId(log.getOrderId());
        updateOrderStatusCommand.setOrderStatus(Order.OrderStatus.OPEN);
        messageProducer.sendToOrderProcessor(updateOrderStatusCommand);
    }

    @Override
    @SneakyThrows
    public void on(OrderMatchLog log) {
        FillOrderCommand fillTakerOrderCommand = new FillOrderCommand();
        fillTakerOrderCommand.setOrderId(log.getTakerOrderId());
        fillTakerOrderCommand.setSide(log.getSide().opposite());
        fillTakerOrderCommand.setProductId(log.getProductId());
        fillTakerOrderCommand.setSize(log.getSize());
        fillTakerOrderCommand.setPrice(log.getPrice());
        fillTakerOrderCommand.setFunds(log.getFunds());
        fillTakerOrderCommand.setTradeId(log.getTradeId());
        messageProducer.sendToOrderProcessor(fillTakerOrderCommand);

        FillOrderCommand fillMakerOrderCommand = new FillOrderCommand();
        fillMakerOrderCommand.setOrderId(log.getMakerOrderId());
        fillMakerOrderCommand.setSide(log.getSide());
        fillMakerOrderCommand.setProductId(log.getProductId());
        fillMakerOrderCommand.setSize(log.getSize());
        fillMakerOrderCommand.setPrice(log.getPrice());
        fillMakerOrderCommand.setFunds(log.getFunds());
        fillMakerOrderCommand.setTradeId(log.getTradeId());
        messageProducer.sendToOrderProcessor(fillMakerOrderCommand);
    }

    @Override
    @SneakyThrows
    public void on(OrderDoneLog log) {
        UpdateOrderStatusCommand updateOrderStatusCommand = new UpdateOrderStatusCommand();
        updateOrderStatusCommand.setOrderId(log.getOrderId());
        updateOrderStatusCommand.setOrderStatus(Order.OrderStatus.CANCELLED);
        updateOrderStatusCommand.setDoneReason(log.getDoneReason());
        messageProducer.sendToOrderProcessor(updateOrderStatusCommand);
    }
}
