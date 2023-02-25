package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.enums.OrderStatus;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.log.OrderDoneLog.DoneReason;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, Log>
        implements ConsumerRebalanceListener, LogHandler {
    private final List<String> productIds;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private final OrderManager orderManager;
    private long uncommittedRecordCount;

    public OrderPersistenceThread(List<String> productIds, KafkaConsumer<String, Log> kafkaConsumer,
                                  KafkaMessageProducer messageProducer, AppProperties appProperties, OrderManager orderManager) {
        super(kafkaConsumer, logger);
        this.productIds = productIds;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
        this.orderManager = orderManager;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
            consumer.commitSync();
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderBookLogTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, Log> record : records) {
            Log log = record.value();
            log.setOffset(record.offset());
            LogDispatcher.dispatch(log, this);
        }

        if (uncommittedRecordCount > 10) {
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }

    @Override
    public void on(OrderRejectedLog log) {
        Order order = new Order();
        order.setOrderId(log.getOrderId());
        order.setProductId(log.getProductId());
        order.setUserId(log.getUserId());
        order.setStatus(OrderStatus.REJECTED);
        order.setPrice(log.getPrice());
        order.setSize(log.getSize());
        order.setFunds(log.getFunds());
        order.setClientOid(log.getClientOid());
        order.setSide(log.getSide());
        order.setType(log.getOrderType());
        order.setTime(log.getTime());
        orderManager.rejectOrder(order);
    }

    @SneakyThrows
    public void on(OrderReceivedLog log) {
        Order order = new Order();
        order.setOrderId(log.getOrderId());
        order.setProductId(log.getProductId());
        order.setUserId(log.getUserId());
        order.setStatus(OrderStatus.RECEIVED);
        order.setPrice(log.getPrice());
        order.setSize(log.getSize());
        order.setFunds(log.getFunds());
        order.setClientOid(log.getClientOid());
        order.setSide(log.getSide());
        order.setType(log.getOrderType());
        order.setTime(log.getTime());
        orderManager.receiveOrder(order);
    }

    @SneakyThrows
    public void on(OrderOpenLog log) {
        orderManager.openOrder(log.getOrderId());
    }

    @SneakyThrows
    public void on(OrderMatchLog log) {
    }

    @SneakyThrows
    public void on(OrderDoneLog log) {
        orderManager.closeOrder(log.getOrderId(),
                log.getDoneReason() == DoneReason.CANCELLED ? OrderStatus.CANCELLED : OrderStatus.FILLED);
    }

    @Override
    public void on(OrderFilledMessage log) {
    }

    @Override
    public void on(AccountChangeLog log) {
    }
}
