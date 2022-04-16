package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class L3OrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private long lastSnapshotSequence;
    private boolean stable;

    public L3OrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
                                        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder().setNameFormat("L3-P-" + productId + "-%s").build());
        this.scheduledExecutor.scheduleWithFixedDelay(this::takeSnapshot, 0,
                appProperties.getL3OrderBookPersistenceInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.scheduledExecutor.shutdown();
    }

    @Override
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        this.stable = stable;
    }

    private void takeSnapshot() {
        if (!stable) {
            return;
        }
        if (orderBook == null || orderBook.getSequence().get() == lastSnapshotSequence) {
            return;
        }

        L3OrderBook l3OrderBook = null;
        if (orderBookLock.tryLock()) {
            try {
                long startTime = System.currentTimeMillis();
                l3OrderBook = new L3OrderBook(orderBook);
                logger.info("l3 order book snapshot ok: elapsedTime={}ms", System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                logger.error("l3 order book snapshot error: {}", e.getMessage(), e);
                return;
            } finally {
                orderBookLock.unlock();
            }
        }

        if (l3OrderBook == null) {
            return;
        }
        lastSnapshotSequence = l3OrderBook.getSequence();

        try {
            orderBookManager.saveL3OrderBook(l3OrderBook);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }
    }
}
