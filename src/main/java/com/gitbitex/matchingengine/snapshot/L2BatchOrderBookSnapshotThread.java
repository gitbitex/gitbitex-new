package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.log.OrderBookLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class L2BatchOrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final Map<String, L2OrderBook> lastL2OrderBookByProductId = new HashMap<>();

    public L2BatchOrderBookSnapshotThread(List<String> productIds, OrderBookManager orderBookManager,
                                          KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                                          AppProperties appProperties) {
        super(productIds, orderBookManager, kafkaConsumer,
                Duration.ofMillis(appProperties.getL2BatchOrderBookPersistenceInterval()), appProperties);
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        super.onPartitionsRevoked(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            lastL2OrderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        super.onPartitionsAssigned(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            L2OrderBook lastL2OrderBook = orderBookManager.getL2BatchOrderBook(productId);
            if (lastL2OrderBook != null) {
                lastL2OrderBookByProductId.put(productId, lastL2OrderBook);
            }
        }
    }

    @Override
    protected void afterRecordProcessed(String productId) {
        OrderBook orderBook = orderBookByProductId.get(productId);
        if (orderBook != null) {
            takeSnapshot(orderBook);
        }
    }

    @Override
    protected void afterRecordsProcessed(int recordCount) {
        if (recordCount == 0) {
            orderBookByProductId.forEach(((productId, orderBook) -> takeSnapshot(orderBook)));
        }
    }

    private void takeSnapshot(OrderBook orderBook) {
        if (!orderBook.isStable()) {
            return;
        }

        L2OrderBook lastL2OrderBook = lastL2OrderBookByProductId.get(orderBook.getProductId());
        if (lastL2OrderBook != null) {
            if (orderBook.getSequence().get() <= lastL2OrderBook.getSequence()) {
                return;
            }
            if (System.currentTimeMillis() - lastL2OrderBook.getTime() < appProperties.getL2BatchOrderBookPersistenceInterval()) {
                return;
            }
        }

        L2OrderBook l2OrderBook = new L2OrderBook(orderBook, appProperties.getL2BatchOrderBookSize());

        try {
            orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }

        lastL2OrderBookByProductId.put(orderBook.getProductId(), l2OrderBook);
    }
}
