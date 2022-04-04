package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaConsumerThread;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.marketmessage.Level2OrderBookSnapshot;
import com.gitbitex.matchingengine.marketmessage.Level2UpdateMessage;
import com.gitbitex.matchingengine.marketmessage.Level3OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class OrderBookSnapshottingThread extends KafkaConsumerThread<String, OrderBookLog>
        implements OrderBookLogHandler {
    private final String productId;
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final SnapshotPersistenceThread snapshotPersistenceThread;
    private final Level2UpdatePublishThread level2UpdatePublishThread;
    private final BlockingQueue<OrderBook> orderBookCopyQueue = new LinkedBlockingQueue<>(10);
    private final BlockingQueue<PageLine> pageLineQueue = new LinkedBlockingQueue<>(1000);
    private final OrderBookLogDispatcher messageDispatcher;
    private final AppProperties appProperties;
    private OrderBook orderBook;

    public OrderBookSnapshottingThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                                       KafkaConsumer<String, OrderBookLog> kafkaConsumer, MarketMessagePublisher marketMessagePublisher,
                                       AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.snapshotPersistenceThread = new SnapshotPersistenceThread(productId, orderBookCopyQueue,
                orderBookSnapshotManager);
        this.level2UpdatePublishThread = new Level2UpdatePublishThread(productId, pageLineQueue,
                marketMessagePublisher);
        this.messageDispatcher = new OrderBookLogDispatcher(this);
        this.appProperties = appProperties;
    }

    @Override
    public void start() {
        super.start();
        this.snapshotPersistenceThread.start();
        this.level2UpdatePublishThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.snapshotPersistenceThread.interrupt();
        this.level2UpdatePublishThread.interrupt();
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookLog> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "." + appProperties.getOrderBookLogTopic()),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        Level3OrderBookSnapshot snapshot = orderBookSnapshotManager.getLevel3Snapshot(
                                productId);
                        orderBook = snapshot != null ? new OrderBook(productId, snapshot) : new OrderBook(productId);

                        for (TopicPartition partition : partitions) {
                            if (orderBook.getOrderBookLogOffset() > 0) {
                                consumer.seek(partition, orderBook.getOrderBookLogOffset() + 1);
                            }
                        }
                    }
                });
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderBookLog> consumer,
                                  ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog orderBookLog = record.value();
            orderBookLog.setOffset(record.offset());
            logger.info("- {} {} {}", record.offset(), orderBookLog.getSequence(), JSON.toJSONString(orderBookLog));

            // check the sequence to ensure that each message is processed in order
            if (orderBookLog.getSequence() <= orderBook.getSequence().get()) {
                logger.info("discard {}", orderBookLog.getSequence());
                continue;
            } else if (orderBook.getSequence().get() + 1 != orderBookLog.getSequence()) {
                throw new RuntimeException("unexpected sequence");
            }

            messageDispatcher.dispatch(orderBookLog);
        }
    }

    @Override
    @SneakyThrows
    public void on(OrderReceivedLog log) {
        PageLine line = orderBook.restoreLog(log);
        offerPageLine(line);
    }

    @Override
    @SneakyThrows
    public void on(OrderOpenLog log) {
        PageLine line = orderBook.restoreLog(log);
        offerPageLine(line);
        takeSnapshot(log);
    }

    @Override
    @SneakyThrows
    public void on(OrderMatchLog log) {
        PageLine line = orderBook.restoreLog(log);
        offerPageLine(line);
        takeSnapshot(log);
    }

    @Override
    @SneakyThrows
    public void on(OrderDoneLog log) {
        PageLine line = orderBook.restoreLog(log);
        offerPageLine(line);
        takeSnapshot(log);
    }

    @SneakyThrows
    private void takeSnapshot(OrderBookLog log) {
        if (log.isCommandFinished()) {
            logger.info("Start copying order book");

            // do not perform the copy operation if queue is full, because copy is a very time-consuming operation and
            // will block the consuming thread
            if (orderBookCopyQueue.remainingCapacity() == 0) {
                logger.warn("orderBookCopyQueue is full");
                return;
            }

            // copy and put in the queue
            orderBookCopyQueue.put(orderBook.copy());


            logger.info("order book copy complete");
        }
    }

    private void offerPageLine(PageLine line) {
        if (line != null) {
            if (!pageLineQueue.offer(line)) {
                logger.warn("pageLineQueue is full");
            }
        }
    }


    @RequiredArgsConstructor
    @Slf4j
    public static class SnapshotPersistenceThread extends Thread {
        private final String productId;
        private final BlockingQueue<OrderBook> orderBookCopyQueue;
        private final OrderBookSnapshotManager orderBookSnapshotManager;
        private final ExecutorService worker = Executors.newFixedThreadPool(8);

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    OrderBook orderBookCopy = orderBookCopyQueue.take();
                    CompletableFuture.allOf(
                            CompletableFuture.runAsync(() -> orderBookSnapshotManager
                                            .saveLevel1BookSnapshot(productId, new Level2OrderBookSnapshot(orderBookCopy, true)),
                                    worker),
                            CompletableFuture.runAsync(() -> orderBookSnapshotManager
                                            .saveLevel2BookSnapshot(productId, new Level2OrderBookSnapshot(orderBookCopy, false)),
                                    worker),
                            CompletableFuture.runAsync(() -> orderBookSnapshotManager
                                    .saveLevel3BookSnapshot(productId, new Level3OrderBookSnapshot(orderBookCopy)), worker)
                    ).join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                }
            }
            logger.info("exiting...");
        }
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
            List<PageLine> lineBuffer = new ArrayList<>(BUF_SIZE);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PageLine line = updatedPageLineQueue.take();

                    lineBuffer.add(line);
                    if (!updatedPageLineQueue.isEmpty() && lineBuffer.size() < BUF_SIZE) {
                        continue;
                    }

                    Level2UpdateMessage level2UpdateMessage = new Level2UpdateMessage(productId, lineBuffer);
                    marketMessagePublisher.publish(level2UpdateMessage);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                } finally {
                    lineBuffer.clear();
                }
            }
            logger.info("exiting...");
        }

    }

}
