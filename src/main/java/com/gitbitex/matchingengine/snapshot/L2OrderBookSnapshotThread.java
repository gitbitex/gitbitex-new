package com.gitbitex.matchingengine.snapshot;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.OrderBookSnapshotManager;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class L2OrderBookSnapshotThread extends OrderBookListener {
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final ThreadPoolExecutor persistenceExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.DAYS,
        new LinkedBlockingQueue<>(10), new ThreadFactoryBuilder().setNameFormat("L2-P-Executor-%s").build());
    private final RTopic l2ChangeTopic;

    public L2OrderBookSnapshotThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
        RedissonClient redissonClient,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookSnapshotManager, kafkaConsumer, appProperties);
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.l2ChangeTopic = redissonClient.getTopic("l2change", StringCodec.INSTANCE);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.persistenceExecutor.shutdown();
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (stable) {
            if (persistenceExecutor.getQueue().remainingCapacity() == 0) {
                logger.warn("persistenceExecutor is busy");
            } else {
                logger.info("start take level2 snapshot");
                L2OrderBookSnapshot snapshot = new L2OrderBookSnapshot(orderBook, false);
                logger.info("done");

                persistenceExecutor.execute(() -> {
                    try {
                        orderBookSnapshotManager.saveLevel2BookSnapshot(snapshot.getProductId(), snapshot);

                        L2OrderBookSnapshot l1Snapshot = snapshot.makeL1OrderBookSnapshot();
                        orderBookSnapshotManager.saveLevel1BookSnapshot(l1Snapshot.getProductId(), l1Snapshot);
                    } catch (Exception e) {
                        logger.error("save snapshot error: {}", e.getMessage(), e);
                    }
                });
            }
        }

        L2OrderBookChange change = new L2OrderBookChange();
        change.setProductId(orderBook.getProductId());
        change.setSide(line.getSide());
        change.setPrice(line.getPrice());
        change.setSize(line.getTotalSize());
        l2ChangeTopic.publish(JSON.toJSONString(change));
    }
}
