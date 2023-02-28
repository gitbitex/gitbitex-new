package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, OrderMessage>
        implements ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final OrderManager orderManager;
    long total;
    private long uncommittedRecordCount;

    public OrderPersistenceThread(KafkaConsumer<String, OrderMessage> kafkaConsumer, AppProperties appProperties,
                                  OrderManager orderManager) {
        super(kafkaConsumer, logger);
        this.appProperties = appProperties;
        this.orderManager = orderManager;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
            //consumer.commitSync();
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
        ConsumerRecords<String, OrderMessage> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, Order> orders = new HashMap<>();
        records.forEach(x -> {
            Order order = order(x.value());
            orders.put(order.getOrderId(), order);
        });

        long t1 = System.currentTimeMillis();
        orderManager.saveAll(orders.values());
        long t2 = System.currentTimeMillis();
        logger.info("orders size: {} time: {}", orders.size(), t2 - t1);
    }

    private Order order(OrderMessage log) {
        Order order = new Order();
        order.setId(log.getOrderId());
        order.setOrderId(log.getOrderId());
        order.setProductId(log.getProductId());
        order.setUserId(log.getUserId());
        order.setStatus(log.getStatus());
        order.setPrice(log.getPrice());
        order.setSize(log.getSize());
        order.setFunds(log.getFunds());
        order.setClientOid(log.getClientOid());
        order.setSide(log.getSide());
        order.setType(log.getType());
        order.setTime(log.getTime());
        order.setCreatedAt(new Date());
        order.setFilledSize(log.getSize().subtract(log.getRemainingSize()));
        order.setExecutedValue(log.getFunds().subtract(log.getRemainingFunds()));
        return order;
    }
}
