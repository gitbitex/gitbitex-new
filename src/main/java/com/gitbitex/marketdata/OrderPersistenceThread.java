package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.matchingengine.log.LogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.log.OrderRejectedLog;
import com.gitbitex.matchingengine.log.TradeMessage;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, Log>
    implements ConsumerRebalanceListener, LogHandler {
    private final AppProperties appProperties;
    private final OrderManager orderManager;
    private long uncommittedRecordCount;

    public OrderPersistenceThread(KafkaConsumer<String, Log> kafkaConsumer, AppProperties appProperties, OrderManager orderManager) {
        super(kafkaConsumer, logger);
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
        consumer.subscribe(Collections.singletonList(appProperties.getOrderMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        consumer.poll(Duration.ofSeconds(5)).forEach(x -> {
            Log log = x.value();
            log.setOffset(x.offset());
            LogDispatcher.dispatch(log, this);
        });
    }

    @Override
    public void on(OrderRejectedLog log) {
    }

    @SneakyThrows
    public void on(OrderReceivedLog log) {
    }

    @SneakyThrows
    public void on(OrderOpenLog log) {
    }

    @SneakyThrows
    public void on(OrderMatchLog log) {
    }

    @SneakyThrows
    public void on(OrderDoneLog log) {
    }

    @Override
    public void on(OrderFilledMessage log) {
    }

    @Override
    public void on(AccountMessage log) {
    }

    @Override
    public void on(OrderMessage log) {
        logger.info(JSON.toJSONString(log));
        Order order = orderManager.findByOrderId(log.getOrderId());
        if (order == null) {
            order = new Order();
        }
        order.setOrderId(log.getOrderId());
        order.setProductId(log.getProductId());
        order.setUserId(log.getUserId());
        order.setStatus(log.getStatus());
        order.setPrice(log.getPrice());
        order.setSize(log.getSize());
        order.setFunds(log.getFunds());
        order.setClientOid(log.getClientOid());
        order.setSide(log.getSide());
        order.setType(log.getOrderType());
        order.setTime(log.getTime());
        order.setCreatedAt(new Date());
        orderManager.save(order);
    }

    @Override
    public void on(TradeMessage message) {

    }
}