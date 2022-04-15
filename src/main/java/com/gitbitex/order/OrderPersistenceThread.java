package com.gitbitex.order;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.account.command.SettleOrderCommand;
import com.gitbitex.account.command.SettleOrderFillCommand;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.command.*;
import com.gitbitex.order.entity.Order;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, OrderCommand>
        implements OrderCommandHandler {
    private final OrderCommandDispatcher orderCommandDispatcher;
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    Set<Long> pendingOffset = new TreeSet<>();
    long uncommitted = 0;

    public OrderPersistenceThread(KafkaConsumer<String, OrderCommand> consumer,
                                  KafkaMessageProducer messageProducer, OrderManager orderManager, AppProperties appProperties) {
        super(consumer, logger);
        this.orderCommandDispatcher = new OrderCommandDispatcher(this);
        this.orderManager = orderManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderCommand> consumer) {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderCommandTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {

            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {

            }
        });
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderCommand> consumer,
                                  ConsumerRecords<String, OrderCommand> records) {
        for (ConsumerRecord<String, OrderCommand> record : records) {
            logger.info("- {}", JSON.toJSONString(record.value()));
            uncommitted++;
            record.value().setOffset(record.offset());
            this.orderCommandDispatcher.dispatch(record.value());
        }
        if (pendingOffset.isEmpty()) {
            consumer.commitSync();
            uncommitted = 0;
        }

        if (uncommitted > 10) {
            logger.info("start commit offset: uncommitted={}", uncommitted);
            while (!pendingOffset.isEmpty()) {
                logger.warn("pending offset not empty");
                continue;
            }
            consumer.commitSync();
            uncommitted=0;
        }
    }

    @Override
    public void on(SaveOrderCommand command) {
        if (orderManager.findByOrderId(command.getOrder().getOrderId()) == null) {
            orderManager.save(command.getOrder());
        }
    }

    @Override
    public void on(UpdateOrderStatusCommand command) {
        Order order = orderManager.findByOrderId(command.getOrderId());
        if (order == null) {
            throw new RuntimeException("order not found: " + command.getOrderId());
        }

        order.setStatus(command.getOrderStatus());
        orderManager.save(order);

        if (order.getStatus() == Order.OrderStatus.FILLED || order.getStatus() == Order.OrderStatus.CANCELLED) {
            SettleOrderCommand settleOrderCommand = new SettleOrderCommand();
            settleOrderCommand.setUserId(order.getUserId());
            settleOrderCommand.setOrderId(order.getOrderId());
            pendingOffset.add(command.getOffset());
            messageProducer.sendToAccountantAsync(settleOrderCommand, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    pendingOffset.remove(command.getOffset());
                }
            });
        }
    }

    @Override
    public void on(FillOrderCommand command) {
        Order order = orderManager.findByOrderId(command.getOrderId());
        if (order == null) {
            throw new RuntimeException("order not found: " + command.getOrderId());
        }

        String fillId = orderManager.fillOrder(command.getOrderId(), command.getTradeId(), command.getSize(),
                command.getPrice(), command.getFunds());

        SettleOrderFillCommand settleOrderFillCommand = new SettleOrderFillCommand();
        settleOrderFillCommand.setUserId(order.getUserId());
        settleOrderFillCommand.setFillId(fillId);
        settleOrderFillCommand.setProductId(order.getProductId());
        pendingOffset.add(command.getOffset());
        messageProducer.sendToAccountantAsync(settleOrderFillCommand, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                pendingOffset.remove(command.getOffset());
            }
        });
    }
}
