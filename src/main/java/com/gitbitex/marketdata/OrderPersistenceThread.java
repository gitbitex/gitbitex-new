package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.OrderEntity;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.OrderMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.*;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final OrderManager orderManager;

    public OrderPersistenceThread(KafkaConsumer<String, Message> kafkaConsumer, OrderManager orderManager,
                                  AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.appProperties = appProperties;
        this.orderManager = orderManager;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {

    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        Map<String, OrderEntity> orders = new HashMap<>();
        records.forEach(x -> {
            Message message = x.value();
            if (message instanceof OrderMessage) {
                OrderEntity order = order((OrderMessage) message);
                orders.put(order.getId(), order);
            }
        });
        orderManager.saveAll(orders.values());

        consumer.commitAsync();
    }

    private OrderEntity order(OrderMessage message) {
        com.gitbitex.matchingengine.Order o = message.getOrder();
        OrderEntity order = new OrderEntity();
        order.setId(o.getId());
        order.setSequence(o.getSequence());
        order.setProductId(o.getProductId());
        order.setUserId(o.getUserId());
        order.setStatus(o.getStatus());
        order.setPrice(o.getPrice());
        order.setSize(o.getSize());
        order.setFunds(o.getFunds());
        order.setClientOid(o.getClientOid());
        order.setSide(o.getSide());
        order.setType(o.getType());
        order.setTime(o.getTime());
        order.setCreatedAt(new Date());
        order.setFilledSize(o.getSize().subtract(o.getRemainingSize()));
        order.setExecutedValue(o.getFunds().subtract(o.getRemainingFunds()));
        return order;
    }
}
