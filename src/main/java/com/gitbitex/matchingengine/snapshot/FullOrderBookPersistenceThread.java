package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.util.SerializationUtils;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FullOrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private long lastSnapshotSequence;
    private boolean stable;

    public FullOrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
                                          KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
                new ThreadFactoryBuilder().setNameFormat("Full-P-" + productId + "-%s").build());
        this.scheduledExecutor.scheduleWithFixedDelay(this::takeSnapshot, 0,
                appProperties.getFullOrderBookPersistenceInterval(), TimeUnit.MILLISECONDS);
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

        byte[] orderBookBytes;
        if (orderBookLock.tryLock()) {
            try {
                long startTime = System.currentTimeMillis();
                orderBookBytes = SerializationUtils.serialize(orderBook);
                if (orderBookBytes == null) {
                    throw new NullPointerException("serialize order book error");
                }
                lastSnapshotSequence = orderBook.getSequence().get();
                logger.info("full order book snapshot ok: elapsedTime={}ms size={}MB", System.currentTimeMillis() - startTime, orderBookBytes.length / 1024.0 / 1024.0);
            } catch (Exception e) {
                logger.error("full order book snapshot error: {}", e.getMessage(), e);
                return;
            } finally {
                orderBookLock.unlock();
            }
        } else {
            return;
        }

        try {
            orderBookManager.saveOrderBook(orderBook.getProductId(), orderBookBytes);
        } catch (Exception e) {
            logger.error("save order book error: {}", e.getMessage(), e);
        }
    }
}
