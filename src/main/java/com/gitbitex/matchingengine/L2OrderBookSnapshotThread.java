package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.marketmessage.Level2OrderBookSnapshot;
import com.gitbitex.matchingengine.marketmessage.Level2UpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class L2OrderBookSnapshotThread extends OrderBookListener {
    private OrderBookSnapshotManager orderBookSnapshotManager;
    private ThreadPoolExecutor snapshotPersistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS, new ArrayBlockingQueue<>(1), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        }
    });

    public L2OrderBookSnapshotThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                                     KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
    }

    @Override
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {

            Level2OrderBookSnapshot snapshot = new Level2OrderBookSnapshot(orderBook.getProductId(),
                    orderBook.getSequence().get(), orderBook.getAsks().getLines(), orderBook.getBids().getLines());
            orderBookSnapshotManager.saveLevel2BookSnapshot(snapshot.getProductId(), snapshot);
        }

        Level2UpdateMessage.Level2UpdateLine change = new Level2UpdateMessage.Level2UpdateLine(line);

    }


    @RequiredArgsConstructor
    @Slf4j
    public static class Level2UpdatePublishThread extends Thread {
        private static final int BUF_SIZE = 100;
        private final String productId;
        private final BlockingQueue<PageLine> updatedPageLineQueue;
        private final MarketMessagePublisher marketMessagePublisher;

        @Override
        public void run() {
            logger.info("starting...");
            List<PageLine> lines = new ArrayList<>(BUF_SIZE);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PageLine line = updatedPageLineQueue.take();

                    // fill the line buffer
                    lines.add(line);
                    if (!updatedPageLineQueue.isEmpty() && lines.size() < BUF_SIZE) {
                        continue;
                    }

                    Level2UpdateMessage level2UpdateMessage = new Level2UpdateMessage(productId, lines);
                    marketMessagePublisher.publish(level2UpdateMessage);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                } finally {
                    lines.clear();
                }
            }
            logger.info("exiting...");
        }

    }


}
