package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.matchingengine.*;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RedissonClient;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class L2OrderBookSnapshotThread extends MessageConsumerThread {
    private final ConcurrentHashMap<String, SimpleOrderBook> orderBooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, L2OrderBook> l2OrderBooks = new ConcurrentHashMap<>();
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final EngineSnapshotManager stateStore;
    private final RedissonClient redissonClient;

    public L2OrderBookSnapshotThread(KafkaConsumer<String, Message> consumer,
                                     OrderBookSnapshotManager orderBookSnapshotManager,
                                     EngineSnapshotManager engineSnapshotManager,
                                     RedissonClient redissonClient,
                                     AppProperties appProperties) {
        super(consumer, appProperties, logger);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.stateStore = engineSnapshotManager;
        this.redissonClient = redissonClient;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // restore engine states
        long messageSequence = 0;
        EngineState engineState = stateStore.getEngineState();
        if (engineState != null) {
            if (engineState.getMessageOffset() != null) {
                this.consumer.seek(partitions.iterator().next(), engineState.getMessageOffset() + 1);
            }
            if (engineState.getMessageSequence() != null) {
                messageSequence = engineState.getMessageSequence();
            }
        }

        // restore order books
        for (Product product : this.stateStore.getProducts()) {
            orderBooks.remove(product.getId());
            for (Order order : stateStore.getOrders(product.getId())) {
                SimpleOrderBook orderBook = getOrderBook(product.getId());
                orderBook.addOrder(order);
            }
        }
    }

    @Override
    protected void processRecords(ConsumerRecords<String, Message> records) {
        records.forEach(x -> {
            Message message = x.value();
            if (message instanceof OrderMessage) {
                Order order = ((OrderMessage) message).getOrder();
                SimpleOrderBook orderBook = getOrderBook(order.getProductId());
                if (order.getStatus() == OrderStatus.OPEN) {
                    orderBook.addOrder(order);
                } else {
                    orderBook.removeOrder(order);
                }
            }
        });
    }

    private SimpleOrderBook getOrderBook(String productId) {
        SimpleOrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new SimpleOrderBook(productId);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    private void takeL2OrderBookSnapshot(SimpleOrderBook orderBook) {
        L2OrderBook l2OrderBook = new L2OrderBook(orderBook, 25);
        orderBookSnapshotManager.saveL2BatchOrderBook(l2OrderBook);
    }
}
