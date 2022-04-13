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
public class FullOrderBookPersistenceThread extends OrderBookListener {
    private final OrderBookManager orderBookManager;
    private final ThreadPoolExecutor persistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS,
        new LinkedBlockingQueue<>(1), new ThreadFactoryBuilder().setNameFormat("Full-P-Executor-%s").build());

    public FullOrderBookPersistenceThread(String productId, OrderBookManager orderBookManager,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.orderBookManager = orderBookManager;
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
                        orderBookManager.saveOrderBook(orderBook.getProductId(), bytes);
                    } catch (Exception e) {
                        logger.error("save snapshot error: {}", e.getMessage(), e);
                    }
                });
            }
        }
    }
}
