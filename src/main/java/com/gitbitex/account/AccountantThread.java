package com.gitbitex.account;

import com.codahale.metrics.MetricRegistry;
import com.gitbitex.AppProperties;
import com.gitbitex.account.command.*;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.kafka.PendingOffsetManager;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.command.OrderBookCommand;
import com.gitbitex.order.entity.Order;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class AccountantThread extends KafkaConsumerThread<String, AccountCommand> {
    private final AccountManager accountManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private final MetricRegistry metricRegistry;
    private final PendingOffsetManager pendingOffsetManager = new PendingOffsetManager();

    public AccountantThread(KafkaConsumer<String, AccountCommand> consumer,
                            AccountManager accountManager,
                            KafkaMessageProducer messageProducer,
                            MetricRegistry metricRegistry,
                            AppProperties appProperties) {
        super(consumer, logger);
        this.accountManager = accountManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
        this.metricRegistry=metricRegistry;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getAccountCommandTopic()),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        for (TopicPartition partition : partitions) {
                            logger.info("partition revoked: {}", partition.toString());
                            pendingOffsetManager.commit(consumer, partition);
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
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, AccountCommand> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());

            pendingOffsetManager.retainOffset(partition, record.offset());
            AccountCommand command = record.value();
            if (command instanceof PlaceOrderCommand) {
                on((PlaceOrderCommand) command, partition, record.offset());
            } else if (command instanceof CancelOrderCommand) {
                on((CancelOrderCommand) command, partition, record.offset());
            } else if (command instanceof SettleOrderFillCommand) {
                on((SettleOrderFillCommand) command, partition, record.offset());
            } else if (command instanceof SettleOrderCommand) {
                on((SettleOrderCommand) command, partition, record.offset());
            } else {
                throw new RuntimeException("unknown command");
            }
            pendingOffsetManager.releaseOffset(partition, record.offset());

            metricRegistry.meter("accountCommand.processed.total").mark();
        }

        pendingOffsetManager.commit(consumer, 1000);
    }


    public void on(PlaceOrderCommand command, TopicPartition partition, long offset) {
        // hold funds for new order
        if (!isLiquidityTrader(command.getOrder().getUserId())) {
            try {
                accountManager.holdFundsForNewOrder(command.getOrder());
            } catch (Exception e) {
                logger.error("hold funds for order failed: {}", e.getMessage(), e);
                command.getOrder().setStatus(Order.OrderStatus.DENIED);
            }
        }

        // send new order to match engine
        NewOrderCommand newOrderCommand = new NewOrderCommand();
        newOrderCommand.setProductId(command.getOrder().getProductId());
        newOrderCommand.setOrder(command.getOrder());
        sendOrderBookCommand(newOrderCommand, partition, offset);
    }


    public void on(CancelOrderCommand command, TopicPartition partition, long offset) {
        com.gitbitex.matchingengine.command.CancelOrderCommand cancelOrderCommand
                = new com.gitbitex.matchingengine.command.CancelOrderCommand();
        cancelOrderCommand.setUserId(command.getUserId());
        cancelOrderCommand.setOrderId(command.getOrderId());
        cancelOrderCommand.setProductId(command.getProductId());
        sendOrderBookCommand(cancelOrderCommand, partition, offset);
    }


    public void on(SettleOrderFillCommand command, TopicPartition partition, long offset) {
        if (isLiquidityTrader(command.getUserId())) {
            return;
        }
        accountManager.settleOrderFill(command.getFillId(), command.getUserId());
    }


    public void on(SettleOrderCommand command, TopicPartition partition, long offset) {
        if (isLiquidityTrader(command.getUserId())) {
            return;
        }
        accountManager.settleOrder(command.getOrderId());
    }

    private boolean isLiquidityTrader(String userId) {
        if (appProperties.getLiquidityTraderUserIds() == null) {
            return false;
        }
        return appProperties.getLiquidityTraderUserIds().contains(userId);
    }

    private void sendOrderBookCommand(OrderBookCommand command, TopicPartition partition, long offset) {
        pendingOffsetManager.retainOffset(partition, offset);
        messageProducer.sendOrderBookCommand(command, (recordMetadata, e) -> {
            if (e != null) {
                logger.error("send order book command error: {}", e.getMessage(), e);
                this.shutdown();
                return;
            }
            pendingOffsetManager.releaseOffset(partition, offset);
        });
    }
}



