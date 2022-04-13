package com.gitbitex.matchingengine.snapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.OrderBookListener;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBookUpdate.Change;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class L2OrderBookUpdatePublishThread extends OrderBookListener {
    private final BlockingQueue<Change> changeQueue = new LinkedBlockingQueue<>(10000);
    private final PublishThread publishThread;

    public L2OrderBookUpdatePublishThread(String productId, OrderBookManager orderBookManager,
        RedissonClient redissonClient,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(productId, orderBookManager, kafkaConsumer, appProperties);
        this.publishThread = new PublishThread(productId, changeQueue,
            redissonClient.getTopic("l2change", StringCodec.INSTANCE));
        this.publishThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.publishThread.interrupt();
    }

    @Override
    @SneakyThrows
    protected void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) {
        if (line != null) {
            Change change = new Change(line.getSide(), line.getPrice(), line.getTotalSize());
            if (!changeQueue.offer(change)) {
                logger.warn("changeQueue is full");
            }
        }
    }

    @RequiredArgsConstructor
    private static class PublishThread extends Thread {
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
