package com.gitbitex.matchingengine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.command.OrderBookCommand;
import com.gitbitex.matchingengine.command.OrderBookCommandDispatcher;
import com.gitbitex.matchingengine.command.OrderBookCommandHandler;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.snapshot.OrderBookSnapshot;
import com.gitbitex.matchingengine.snapshot.OrderBookSnapshotManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, OrderBookCommand> implements OrderBookCommandHandler {
    private final String productId;
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final OrderBookLogPersistenceThread orderBookLogPersistenceThread;
    private final OrderBookCommandDispatcher orderBookCommandDispatcher;
    private final BlockingQueue<OrderBookLog> orderBookLogQueue = new LinkedBlockingQueue<>(10000);
    private final AppProperties appProperties;
    private OrderBook orderBook;

    public MatchingThread(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
        KafkaConsumer<String, OrderBookCommand> messageKafkaConsumer, KafkaMessageProducer messageProducer,
        AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.productId = productId;
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.orderBookLogPersistenceThread = new OrderBookLogPersistenceThread(orderBookLogQueue, messageProducer,
            appProperties);
        this.orderBookCommandDispatcher = new OrderBookCommandDispatcher(this);
        this.appProperties = appProperties;
    }

    @Override
    public void start() {
        super.start();
        this.orderBookLogPersistenceThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.orderBookLogPersistenceThread.interrupt();
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookCommand> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookCommandTopic()),
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    for (TopicPartition partition : partitions) {
                        logger.warn("partition revoked: {}", partition);
                    }
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    OrderBookSnapshot snapshot = orderBookSnapshotManager.getOrderBookSnapshot(productId);
                    orderBook = snapshot != null
                        ? snapshot.restore()
                        : new OrderBook(productId);

                    for (TopicPartition partition : partitions) {
                        if (snapshot != null) {
                            consumer.seek(partition, snapshot.getCommandOffset() + 1);
                        }
                    }
                }
            });
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderBookCommand> consumer,
        ConsumerRecords<String, OrderBookCommand> records) {
        for (ConsumerRecord<String, OrderBookCommand> record : records) {
            OrderBookCommand orderBookCommand = record.value();
            orderBookCommand.setOffset(record.offset());
            logger.info("- {}", JSON.toJSONString(orderBookCommand));
            this.orderBookCommandDispatcher.dispatch(orderBookCommand);
        }
    }

    @Override
    @SneakyThrows
    public void on(NewOrderCommand command) {
        List<OrderBookLog> logs = orderBook.executeCommand(command);
        if (logs != null) {
            for (OrderBookLog log : logs) {
                checkOrderBookLogQueueCapacity();
                orderBookLogQueue.put(log);
            }
        }
    }

    @Override
    @SneakyThrows
    public void on(CancelOrderCommand command) {
        OrderBookLog log = orderBook.executeCommand(command);
        if (log != null) {
            checkOrderBookLogQueueCapacity();
            orderBookLogQueue.put(log);
        }
    }

    private void checkOrderBookLogQueueCapacity() {
        if (orderBookLogQueue.remainingCapacity() == 0) {
            logger.warn("orderBookLogQueue queue is full, matching thread may block");
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class OrderBookLogPersistenceThread extends Thread {
        private final BlockingQueue<OrderBookLog> orderBookLogQueue;
        private final KafkaMessageProducer messageProducer;
        private final AppProperties appProperties;

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    OrderBookLog log = orderBookLogQueue.take();
                    sendOrderBookLog(log);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            logger.warn("exiting...");
        }

        @SneakyThrows
        public void sendOrderBookLog(OrderBookLog log) {
            if (log.getProductId() == null) {
                throw new NullPointerException("bad OrderBookLog: productId is null");
            }

            String topic = log.getProductId() + "-" + appProperties.getOrderBookLogTopic();
            String key = log.getProductId();
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, JSON.toJSONString(log));
            messageProducer.send(record).get();
        }
    }

}
