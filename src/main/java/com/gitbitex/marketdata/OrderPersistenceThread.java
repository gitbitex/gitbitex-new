package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.matchingengine.message.OrderMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, OrderMessage>
        implements ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final OrderManager orderManager;

    public OrderPersistenceThread(KafkaConsumer<String, OrderMessage> kafkaConsumer, OrderManager orderManager,
                                  AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.appProperties = appProperties;
        this.orderManager = orderManager;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
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
        var records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, Order> orders = new HashMap<>();
        records.forEach(x -> {
            Order order = order(x.value());
            orders.put(order.getId(), order);
        });

        long t1 = System.currentTimeMillis();
        orderManager.saveAll(orders.values());
        logger.info("saved {} order(s) ({}ms)", orders.size(), System.currentTimeMillis() - t1);

        consumer.commitSync();
    }

    private Order order(OrderMessage message) {
        Order order = new Order();
        order.setId(message.getId());
        order.setSequence(message.getSequence());
        order.setProductId(message.getProductId());
        order.setUserId(message.getUserId());
        order.setStatus(message.getStatus());
        order.setPrice(message.getPrice());
        order.setSize(message.getSize());
        order.setFunds(message.getFunds());
        order.setClientOid(message.getClientOid());
        order.setSide(message.getSide());
        order.setType(message.getType());
        order.setTime(message.getTime());
        order.setCreatedAt(new Date());
        order.setFilledSize(message.getSize().subtract(message.getRemainingSize()));
        order.setExecutedValue(message.getFunds().subtract(message.getRemainingFunds()));
        return order;
    }
}
