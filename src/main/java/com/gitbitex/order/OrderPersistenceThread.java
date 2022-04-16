package com.gitbitex.order;

import com.gitbitex.AppProperties;
import com.gitbitex.account.command.AccountCommand;
import com.gitbitex.account.command.SettleOrderCommand;
import com.gitbitex.account.command.SettleOrderFillCommand;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.kafka.PendingOffsetManager;
import com.gitbitex.order.command.FillOrderCommand;
import com.gitbitex.order.command.OrderCommand;
import com.gitbitex.order.command.SaveOrderCommand;
import com.gitbitex.order.command.UpdateOrderStatusCommand;
import com.gitbitex.order.entity.Order;
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
public class OrderPersistenceThread extends KafkaConsumerThread<String, OrderCommand> {
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private final PendingOffsetManager pendingOffsetManager = new PendingOffsetManager();

    public OrderPersistenceThread(KafkaConsumer<String, OrderCommand> consumer,
                                  KafkaMessageProducer messageProducer, OrderManager orderManager, AppProperties appProperties) {
        super(consumer, logger);
        this.orderManager = orderManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderCommandTopic()), new ConsumerRebalanceListener() {
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
    protected void processRecords(ConsumerRecords<String, OrderCommand> records) {
        for (ConsumerRecord<String, OrderCommand> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());

            pendingOffsetManager.retainOffset(partition, record.offset());
            OrderCommand command = record.value();
            if (command instanceof SaveOrderCommand) {
                on((SaveOrderCommand) command, partition, record.offset());
            } else if (command instanceof UpdateOrderStatusCommand) {
                on((UpdateOrderStatusCommand) command, partition, record.offset());
            } else if (command instanceof FillOrderCommand) {
                on((FillOrderCommand) command, partition, record.offset());
            } else {
                throw new RuntimeException("unknown command");
            }
            pendingOffsetManager.releaseOffset(partition, record.offset());
        }

        pendingOffsetManager.commit(consumer);
    }


    public void on(SaveOrderCommand command, TopicPartition partition, long offset) {
        if (orderManager.findByOrderId(command.getOrder().getOrderId()) == null) {
            orderManager.save(command.getOrder());
        }
    }

    public void on(UpdateOrderStatusCommand command, TopicPartition partition, long offset) {
        Order order = orderManager.findByOrderId(command.getOrderId());
        if (order == null) {
            throw new RuntimeException("order not found: " + command.getOrderId());
        }
        if (order.getStatus() == command.getOrderStatus()) {
            logger.warn("The order is already in cancelled status: {}", command.getOrderId());
        }

        // update order status
        order.setStatus(command.getOrderStatus());
        orderManager.save(order);

        // if the order has been filled or cancelled, notify the account to settle
        if (order.getStatus() == Order.OrderStatus.FILLED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            SettleOrderCommand settleOrderCommand = new SettleOrderCommand();
            settleOrderCommand.setUserId(order.getUserId());
            settleOrderCommand.setOrderId(order.getOrderId());
            sendAccountCommand(settleOrderCommand, partition, offset);
        }
    }

    public void on(FillOrderCommand command, TopicPartition partition, long offset) {
        Order order = orderManager.findByOrderId(command.getOrderId());
        if (order == null) {
            throw new RuntimeException("order not found: " + command.getOrderId());
        }

        // fill order
        String fillId = orderManager.fillOrder(command.getOrderId(), command.getTradeId(), command.getSize(),
                command.getPrice(), command.getFunds());

        // notify account to settle
        SettleOrderFillCommand settleOrderFillCommand = new SettleOrderFillCommand();
        settleOrderFillCommand.setUserId(order.getUserId());
        settleOrderFillCommand.setFillId(fillId);
        settleOrderFillCommand.setProductId(order.getProductId());
        sendAccountCommand(settleOrderFillCommand, partition, offset);
    }

    private void sendAccountCommand(AccountCommand command, TopicPartition partition, long offset) {
        pendingOffsetManager.retainOffset(partition, offset);
        messageProducer.sendAccountCommand(command, (recordMetadata, e) -> {
            if (e != null) {
                logger.error("send account command error: {}", e.getMessage(), e);
                this.shutdown();
                return;
            }
            pendingOffsetManager.releaseOffset(partition, offset);
        });
    }
}
