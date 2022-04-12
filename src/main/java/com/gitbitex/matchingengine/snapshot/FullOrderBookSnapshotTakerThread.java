package com.gitbitex.matchingengine.snapshot;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
public class FullOrderBookSnapshotTakerThread extends OrderBookListener {
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final ThreadPoolExecutor persistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS,
        new LinkedBlockingQueue<>(10), new ThreadFactoryBuilder().setNameFormat("Full-P-Executor-%s").build());

    public FullOrderBookSnapshotTakerThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (persistenceExecutor.getQueue().remainingCapacity() == 0) {
                logger.warn("persistenceExecutor is busy");
            } else {
                logger.info("start take full snapshot");
                byte[] bytes = SerializationUtils.serialize(orderBook);
                logger.info("done");

                persistenceExecutor.execute(() -> {
                    try {
                        orderBookSnapshotManager.saveOrderBookSnapshot(orderBook.getProductId(), bytes);
                    } catch (Exception e) {
                        logger.error("save snapshot error: {}", e.getMessage(), e);
                    }
                });
            }
        }
    }
}
