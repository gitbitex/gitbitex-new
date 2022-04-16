package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class L2BatchOrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final AppProperties appProperties;
    private long lastSnapshotTime;
    private long lastSnapshotSequence;
    private boolean stable;

    public L2BatchOrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
                                             KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                                             AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder().setNameFormat("L2-Batch-P-" + productId + "-%s").build());
        this.scheduledExecutor.scheduleWithFixedDelay(this::takeSnapshot, 0, 1, TimeUnit.SECONDS);
        this.appProperties = appProperties;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.scheduledExecutor.shutdown();
    }

    @Override
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        this.stable = stable;
        takeSnapshot();
    }

    private void takeSnapshot() {
        if (!stable) {
            return;
        }
        if (orderBook == null || orderBook.getSequence().get() == lastSnapshotSequence) {
            return;
        }
        if (System.currentTimeMillis() - lastSnapshotTime < appProperties.getL2BatchOrderBookPersistenceInterval()) {
            return;
        }

        L2OrderBook l2OrderBook;
        if (orderBookLock.tryLock()) {
            try {
                l2OrderBook = new L2OrderBook(orderBook, appProperties.getL2BatchOrderBookSize());
                lastSnapshotSequence = l2OrderBook.getSequence();
                lastSnapshotTime = System.currentTimeMillis();
            } catch (Exception e) {
                logger.error("snapshot error: {}", e.getMessage(), e);
                return;
            } finally {
                orderBookLock.unlock();
            }
        } else {
            return;
        }

        try {
            orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }
    }
}
