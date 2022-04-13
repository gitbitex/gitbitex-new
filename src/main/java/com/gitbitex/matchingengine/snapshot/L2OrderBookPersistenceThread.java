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

@Slf4j
public class L2OrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ScheduledThreadPoolExecutor scheduledExecutor;
    private final ReentrantLock lock = new ReentrantLock(true);
    private long lastSnapshotSequence;
    private OrderBook orderBook;

    public L2OrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("L2-P-Executor-" + productId + "-%s").build());
        this.scheduledExecutor.scheduleWithFixedDelay(this::takeSnapshot, 0,
            appProperties.getL2OrderBookPersistenceInterval(), TimeUnit.MILLISECONDS);
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

            // level2
            logger.info("start take level2 snapshot");
            L2OrderBook l2OrderBook = new L2OrderBook(orderBook);
            logger.info("done");
            orderBookManager.saveL2OrderBook(l2OrderBook.getProductId(), l2OrderBook);

            // level1
            L2OrderBook l1OrderBook = l2OrderBook.makeL1OrderBookSnapshot();
            orderBookManager.saveL1OrderBook(l1OrderBook.getProductId(), l1OrderBook);

            lastSnapshotSequence = l2OrderBook.getSequence();
            orderBook = null;
        } catch (Exception e) {
            logger.error("snapshot error: {}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }
}
