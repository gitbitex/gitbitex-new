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
public class FullOrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final Map<String, FullOrderBookSnapshot> lastSnapshotByProductId = new HashMap<>();

    public FullOrderBookSnapshotThread(List<String> productIds, OrderBookManager orderBookManager,
                                       KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                                       AppProperties appProperties) {
        super(productIds, orderBookManager, kafkaConsumer,
                Duration.ofMillis(appProperties.getFullOrderBookPersistenceInterval()), appProperties);
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        super.onPartitionsRevoked(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            OrderBook orderBook = orderBookByProductId.get(productId);
            if (orderBook != null) {
                takeSnapshot(orderBook, false);
            }
            lastSnapshotByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        super.onPartitionsAssigned(partitions);
        for (TopicPartition partition : partitions) {
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            FullOrderBookSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot(productId);
            if (snapshot != null) {
                lastSnapshotByProductId.put(productId, snapshot);
            }
        }
    }

    @Override
    protected void doPoll() {
        super.doPoll();
        orderBookByProductId.forEach(((productId, orderBook) -> takeSnapshot(orderBook, true)));
    }

    private void takeSnapshot(OrderBook orderBook, boolean checkInterval) {
        if (!orderBook.isStable()) {
            return;
        }

        FullOrderBookSnapshot lastSnapshot = lastSnapshotByProductId.get(orderBook.getProductId());
        if (lastSnapshot != null) {
            if (orderBook.getSequence().get() <= lastSnapshot.getSequence()) {
                return;
            }
            if (checkInterval && System.currentTimeMillis() - lastSnapshot.getTime() < appProperties.getFullOrderBookPersistenceInterval()) {
                return;
            }
        }

        long startTime = System.currentTimeMillis();
        FullOrderBookSnapshot snapshot = new FullOrderBookSnapshot(orderBook);
        logger.info("full order book snapshot ok: {} elapsedTime={}ms size={}MB", orderBook.getProductId(),
                System.currentTimeMillis() - startTime, snapshot.getBase64Data().getBytes().length / 1024.0 / 1024.0);

        try {
            orderBookManager.saveFullOrderBookSnapshot(snapshot);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }

        lastSnapshotByProductId.put(snapshot.getProductId(), snapshot);
    }
}
