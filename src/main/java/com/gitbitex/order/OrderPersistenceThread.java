package com.gitbitex.order;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.account.command.SettleOrderCommand;
import com.gitbitex.account.command.SettleOrderFillCommand;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.command.FillOrderCommand;
import com.gitbitex.order.command.OrderCommand;
import com.gitbitex.order.command.OrderCommandDispatcher;
import com.gitbitex.order.command.OrderCommandHandler;
import com.gitbitex.order.command.SaveOrderCommand;
import com.gitbitex.order.command.UpdateOrderStatusCommand;
import com.gitbitex.order.entity.Order;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, OrderCommand>
    implements OrderCommandHandler {
    private final OrderCommandDispatcher orderCommandDispatcher;
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    AtomicLong minCommittableOffset;

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
        consumer.subscribe(Collections.singletonList(appProperties.getOrderCommandTopic()));
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderCommand> consumer,
        ConsumerRecords<String, OrderCommand> records) {
        for (ConsumerRecord<String, OrderCommand> record : records) {
            logger.info("- {}", JSON.toJSONString(record.value()));
            minCommittableOffset.incrementAndGet();
            this.orderCommandDispatcher.dispatch(record.value());
        }
        consumer.commitSync();
    }

    @Override
    public void on(SaveOrderCommand command) {
        if (orderManager.findByOrderId(command.getOrder().getOrderId()) == null) {
            orderManager.save(command.getOrder());
        }
        minCommittableOffset.decrementAndGet();
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
            messageProducer.sendToAccountant(settleOrderCommand);
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
        messageProducer.sendToAccountant(settleOrderFillCommand);
    }
}
