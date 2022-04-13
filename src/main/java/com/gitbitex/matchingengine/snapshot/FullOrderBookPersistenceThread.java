package com.gitbitex.matchingengine.snapshot;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.util.SerializationUtils;

@Slf4j
public class FullOrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final ReentrantLock lock = new ReentrantLock(true);
    private long lastSnapshotSequence;
    private OrderBook orderBook;

    public FullOrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("L3-P-Executor-" + productId + "-%s").build());
        this.scheduledExecutor.scheduleWithFixedDelay(this::takeSnapshot, 0,
            appProperties.getFullOrderBookPersistenceInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.scheduledExecutor.shutdown();
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            lock.lock();
            this.orderBook = orderBook;
            lock.unlock();
        }
    }

    private void takeSnapshot() {
        lock.lock();
        try {
            if (orderBook == null) {
                return;
            }
            if (orderBook.getSequence().get() == lastSnapshotSequence) {
                return;
            }

            // full
            logger.info("start take full snapshot");
            byte[] orderBookBytes = SerializationUtils.serialize(orderBook);
            logger.info("done");
            orderBookManager.saveOrderBook(orderBook.getProductId(), orderBookBytes);

            lastSnapshotSequence = orderBook.getSequence().get();
            orderBook = null;
        } catch (Exception e) {
            logger.error("snapshot error: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}
