package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class FullOrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final BlockingQueue<OrderBookSnapshot> orderBookSnapshotQueue = new LinkedBlockingQueue<>(10);

    public FullOrderBookSnapshotThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                                       KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (orderBookSnapshotQueue.remainingCapacity() == 0) {
                logger.warn("orderBookSnapshotQueue is full");
            } else {
                OrderBookSnapshot snapshot = new OrderBookSnapshot(orderBook);
                orderBookSnapshotQueue.put(snapshot);
            }
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    public static class SnapshotPersistenceThread extends Thread {
        private final String productId;
        private final BlockingQueue<OrderBookSnapshot> orderBookSnapshotQueue;
        private final OrderBookSnapshotManager orderBookSnapshotManager;

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    OrderBookSnapshot snapshot = orderBookSnapshotQueue.take();
                    orderBookSnapshotManager.saveOrderBookSnapshot(productId, snapshot);
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
