package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.marketmessage.Level3OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class L3OrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final BlockingQueue<Level3OrderBookSnapshot> l3OrderBookSnapshotQueue = new LinkedBlockingQueue<>(10);

    public L3OrderBookSnapshotThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                                     KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (l3OrderBookSnapshotQueue.remainingCapacity() == 0) {
                logger.warn("l3OrderBookSnapshotQueue is full");
            } else {
                Level3OrderBookSnapshot snapshot = new Level3OrderBookSnapshot(orderBook);
                l3OrderBookSnapshotQueue.put(snapshot);
            }
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    public static class SnapshotPersistenceThread extends Thread {
        private final String productId;
        private final BlockingQueue<Level3OrderBookSnapshot> l3OrderBookSnapshotQueue;
        private final OrderBookSnapshotManager orderBookSnapshotManager;

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Level3OrderBookSnapshot snapshot = l3OrderBookSnapshotQueue.take();
                    orderBookSnapshotManager.saveLevel3BookSnapshot(productId, snapshot);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                }
            }
            logger.info("exiting...");
        }
    }

}
