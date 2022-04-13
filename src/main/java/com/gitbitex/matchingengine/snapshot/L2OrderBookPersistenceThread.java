package com.gitbitex.matchingengine.snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBookUpdate.Change;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class L2OrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ThreadPoolExecutor persistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS,
        new LinkedBlockingQueue<>(10), new ThreadFactoryBuilder().setNameFormat("L2-P-Executor-%s").build());
    private final BlockingQueue<Change> changeQueue = new LinkedBlockingQueue<>(10000);
    private final L2OrderBookUpdatePublishThread l2OrderBookUpdatePublishThread;
    private final AppProperties appProperties;
    private long lastSnapshotTime;

    public L2OrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
        RedissonClient redissonClient,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.appProperties=appProperties;
        this.l2OrderBookUpdatePublishThread = new L2OrderBookUpdatePublishThread(productId, changeQueue,
            redissonClient.getTopic("l2change", StringCodec.INSTANCE));
        this.l2OrderBookUpdatePublishThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.persistenceExecutor.shutdown();
        this.l2OrderBookUpdatePublishThread.interrupt();
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (System.currentTimeMillis() - lastSnapshotTime < appProperties.getL2OrderBookPersistenceInterval()) {
                return;
            } else if (persistenceExecutor.getQueue().remainingCapacity() == 0) {
                logger.warn("persistenceExecutor is busy");
                return;
            } else {
                logger.info("start take level2 snapshot");
                L2OrderBook snapshot = new L2OrderBook(orderBook);
                lastSnapshotTime = System.currentTimeMillis();
                logger.info("done");

                persistenceExecutor.execute(() -> {
                    try {
                        orderBookManager.saveL2OrderBook(snapshot.getProductId(), snapshot);

                        L2OrderBook l1Snapshot = snapshot.makeL1OrderBookSnapshot();
                        orderBookManager.saveL1OrderBook(l1Snapshot.getProductId(), l1Snapshot);
                    } catch (Exception e) {
                        logger.error("save snapshot error: {}", e.getMessage(), e);
                    }
                });
            }
        }

        if (line != null) {
            Change change = new Change(line.getSide(), line.getPrice(), line.getTotalSize());
            changeQueue.offer(change);
        }
    }

    @RequiredArgsConstructor
    private static class L2OrderBookUpdatePublishThread extends Thread {
        private final static int BUF_SIZE = 100;
        private final String productId;
        private final BlockingQueue<Change> changeQueue;
        private final RTopic l2UpdateTopic;
        private final Map<String, Change> changeBuffer = new LinkedHashMap<>(BUF_SIZE);

        @Override

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Change change = changeQueue.take();

                    changeBuffer.put(makeBufferKey(change), change);
                    if (changeBuffer.size() < BUF_SIZE && !changeQueue.isEmpty()) {
                        continue;
                    }

                    L2OrderBookUpdate l2OrderBookUpdate = new L2OrderBookUpdate();
                    l2OrderBookUpdate.setProductId(productId);
                    l2OrderBookUpdate.setChanges(changeBuffer.values());
                    l2UpdateTopic.publish(JSON.toJSONString(l2OrderBookUpdate));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                } finally {
                    changeBuffer.clear();
                }
            }
        }

        private String makeBufferKey(Change change) {
            return change.get(0) + "-" + change.get(1);
        }
    }
}
