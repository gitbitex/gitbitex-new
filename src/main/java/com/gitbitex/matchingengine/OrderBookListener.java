package com.gitbitex.matchingengine;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderBookLogDispatcher;
import com.gitbitex.matchingengine.log.OrderBookLogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public abstract class OrderBookListener extends KafkaConsumerThread<String, OrderBookLog>
    implements OrderBookLogHandler {
    protected final ReentrantLock orderBookLock;
    private final String productId;
    private final OrderBookManager orderBookManager;
    private final OrderBookLogDispatcher messageDispatcher;
    private final AppProperties appProperties;
    protected OrderBook orderBook;

    public OrderBookListener(String productId, OrderBookManager orderBookManager,
        KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.orderBookManager = orderBookManager;
        this.messageDispatcher = new OrderBookLogDispatcher(this);
        this.appProperties = appProperties;
        this.orderBookLock = new ReentrantLock();
    }

    @Override
    protected void doSubscribe( ) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()),
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    orderBook = orderBookManager.getOrderBook(productId);
                    if (orderBook != null) {
                        for (TopicPartition partition : partitions) {
                            consumer.seek(partition, orderBook.getLogOffset() + 1);
                        }
                    } else {
                        orderBook = new OrderBook(productId);
                    }
                }
            });
    }

    @Override
    protected void processRecords(
        ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog orderBookLog = record.value();
            orderBookLog.setOffset(record.offset());
            //logger.info("- {} {} {}", record.offset(), orderBookLog.getSequence(), JSON.toJSONString(orderBookLog));

            // check the sequence to ensure that each message is processed in order
            if (orderBookLog.getSequence() <= orderBook.getSequence().get()) {
                logger.info("discard {}", orderBookLog.getSequence());
                continue;
            } else if (orderBook.getSequence().get() + 1 != orderBookLog.getSequence()) {
                throw new RuntimeException("unexpected sequence");
            }

            messageDispatcher.dispatch(orderBookLog);
        }
    }

    @Override
    public void on(OrderReceivedLog log) {
        orderBookLock.lock();
        try {
            PageLine line = orderBook.restoreLog(log);
            onOrderBookChange(orderBook, log.isCommandFinished(), line);
        } finally {
            orderBookLock.unlock();
        }
    }

    @Override
    public void on(OrderOpenLog log) {
        orderBookLock.lock();
        try {
            PageLine line = orderBook.restoreLog(log);
            onOrderBookChange(orderBook, log.isCommandFinished(), line);
        } finally {
            orderBookLock.unlock();
        }
    }

    @Override
    public void on(OrderMatchLog log) {
        orderBookLock.lock();
        try {
            PageLine line = orderBook.restoreLog(log);
            onOrderBookChange(orderBook, log.isCommandFinished(), line);
        } finally {
            orderBookLock.unlock();
        }
    }

    @Override
    public void on(OrderDoneLog log) {
        orderBookLock.lock();
        try {
            PageLine line = orderBook.restoreLog(log);
            onOrderBookChange(orderBook, log.isCommandFinished(), line);
        } finally {
            orderBookLock.unlock();
        }
    }

    protected abstract void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line);

}
