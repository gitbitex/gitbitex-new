package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.marketmessage.L2OrderBookSnapshot;
import com.gitbitex.matchingengine.marketmessage.L2UpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class L2OrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final ThreadPoolExecutor l2UpdatePublishExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS, new LinkedBlockingQueue<>(10));
    private final ThreadPoolExecutor persistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS, new LinkedBlockingQueue<>(10));
    private final BlockingQueue<L2UpdateMessage.L2Change> l2ChangeQueue = new LinkedBlockingQueue<>(1000);

    public L2OrderBookSnapshotThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                                     KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties, MarketMessagePublisher marketMessagePublisher) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.l2UpdatePublishExecutor.execute(new Level2UpdatePublishRunnable(productId, l2ChangeQueue, marketMessagePublisher));
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (persistenceExecutor.getQueue().remainingCapacity() == 0) {
                logger.warn("persistenceExecutor is full");
            } else {
                logger.info("start take level2 snapshot");
                L2OrderBookSnapshot snapshot = new L2OrderBookSnapshot(orderBook, false);
                logger.info("done");

                persistenceExecutor.execute(() -> {
                    try {
                        orderBookSnapshotManager.saveLevel2BookSnapshot(snapshot.getProductId(), snapshot);

                        L2OrderBookSnapshot l1Snapshot = snapshot.makeL1OrderBookSnapshot();
                        orderBookSnapshotManager.saveLevel1BookSnapshot(l1Snapshot.getProductId(), l1Snapshot);
                    } catch (Exception e) {
                        logger.error("save snapshot error: {}", e.getMessage(), e);
                    }
                });
            }
        }

        L2UpdateMessage.L2Change change = new L2UpdateMessage.L2Change(line);
        if (!l2ChangeQueue.offer(change)) {
            logger.warn("offer failed, l2ChangeQueue is full");
        }
    }


    @RequiredArgsConstructor
    @Slf4j
    public static class Level2UpdatePublishRunnable implements Runnable {
        private static final int BUF_SIZE = 100;
        private final String productId;
        private final BlockingQueue<L2UpdateMessage.L2Change> l2ChangeQueue;
        private final MarketMessagePublisher marketMessagePublisher;

        @Override
        public void run() {
            logger.info("starting...");
            List<L2UpdateMessage.L2Change> changes = new ArrayList<>(BUF_SIZE);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    L2UpdateMessage.L2Change change = l2ChangeQueue.take();

                    // fill the line buffer
                    changes.add(change);
                    if (!l2ChangeQueue.isEmpty() && changes.size() < BUF_SIZE) {
                        continue;
                    }

                    L2UpdateMessage l2UpdateMessage = new L2UpdateMessage(productId, changes);
                    marketMessagePublisher.publish(l2UpdateMessage);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                } finally {
                    changes.clear();
                }
            }
            logger.info("exiting...");
        }

    }


}
