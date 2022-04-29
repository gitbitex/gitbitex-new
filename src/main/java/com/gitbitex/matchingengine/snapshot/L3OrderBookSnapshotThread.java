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
public class L3OrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final Map<String, L3OrderBook> lastL3OrderBookByProductId = new HashMap<>();

    public L3OrderBookSnapshotThread(List<String> productIds, OrderBookManager orderBookManager,
                                     KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                                     AppProperties appProperties) {
        super(productIds, orderBookManager, kafkaConsumer,
                Duration.ofMillis(appProperties.getL3OrderBookPersistenceInterval()), appProperties);
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        super.onPartitionsRevoked(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            lastL3OrderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        super.onPartitionsAssigned(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            L3OrderBook lastL3OrderBook = orderBookManager.getL3OrderBook(productId);
            if (lastL3OrderBook != null) {
                lastL3OrderBookByProductId.put(productId, lastL3OrderBook);
            }
        }
    }

    @Override
    protected void doPoll() {
        super.doPoll();
        orderBookByProductId.forEach(((productId, orderBook) -> takeSnapshot(orderBook)));
    }

    private void takeSnapshot(OrderBook orderBook) {
        if (!orderBook.isStable()) {
            return;
        }

        L3OrderBook lastL3OrderBook = lastL3OrderBookByProductId.get(orderBook.getProductId());
        if (lastL3OrderBook != null) {
            if (orderBook.getSequence().get() <= lastL3OrderBook.getSequence()) {
                return;
            }
            if (System.currentTimeMillis() - lastL3OrderBook.getTime() < appProperties.getL3OrderBookPersistenceInterval()) {
                return;
            }
        }

        long startTime = System.currentTimeMillis();
        L3OrderBook l3OrderBook = new L3OrderBook(orderBook);
        logger.info("l3 order book snapshot ok: {} elapsedTime={}ms", orderBook.getProductId(), System.currentTimeMillis() - startTime);

        try {
            orderBookManager.saveL3OrderBook(l3OrderBook);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }

        lastL3OrderBookByProductId.put(orderBook.getProductId(), l3OrderBook);
    }
}
