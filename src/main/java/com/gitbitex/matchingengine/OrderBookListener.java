package com.gitbitex.matchingengine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.matchingengine.log.LogHandler;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class OrderBookListener extends KafkaConsumerThread<String, Log>
    implements LogHandler, ConsumerRebalanceListener {
    protected final Map<String, OrderBook> orderBookByProductId = new HashMap<>();
    private final List<String> productIds;
    private final OrderBookManager orderBookManager;

    private final AppProperties appProperties;
    private final Duration pollTimeout;
    private long sequence;

    public OrderBookListener(List<String> productIds, OrderBookManager orderBookManager,
        KafkaConsumer<String, Log> kafkaConsumer,
        Duration pollTimeout,
        AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productIds = productIds;
        this.orderBookManager = orderBookManager;

        this.appProperties = appProperties;
        this.pollTimeout = pollTimeout;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
            //String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            //orderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            /*String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            OrderBook orderBook;
            FullOrderBookSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot(productId);
            if (snapshot != null) {
                orderBook =new OrderBook(snapshot);
                consumer.seek(partition, orderBook.getLogOffset() + 1);
            } else {
                orderBook = new OrderBook(productId);
            }
            orderBookByProductId.put(productId, orderBook);*/
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderCommandTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(pollTimeout);

        for (ConsumerRecord<String, Log> record : records) {
            Log log = record.value();
            log.setOffset(record.offset());

            if (log.getSequence() <= sequence) {
                logger.warn("discard log sequence= {}", log.getSequence());
                continue;
            } else if (log.getSequence() != sequence + 1) {
                throw new RuntimeException("unexpected sequence:" + log.getSequence());
            }
            sequence = log.getSequence();

            logger.info("{}", JSON.toJSONString(log));
            LogDispatcher.dispatch(log, this);

        }

        afterRecordsProcessed(records.count());
    }

    protected void afterRecordProcessed(String productId) {
    }

    protected void afterRecordsProcessed(int recordCount) {
    }

    @Override
    public void on(OrderRejectedMessage log) {

    }

    @Override
    public void on(OrderReceivedMessage log) {
        OrderBook orderBook = orderBookByProductId.get(log.getProductId());
        if (orderBook == null) {
            orderBook = new OrderBook(log.getProductId(), null, null, new AtomicLong());
            orderBookByProductId.put(log.getProductId(),orderBook);
        }
        orderBook.restoreLog(log);
    }

    @Override
    public void on(OrderOpenMessage log) {
        OrderBook orderBook = orderBookByProductId.get(log.getProductId());
        if (orderBook == null) {
            orderBook = new OrderBook(log.getProductId(), null, null, new AtomicLong());
            orderBookByProductId.put(log.getProductId(),orderBook);
        }
        orderBook.restoreLog(log);
    }

    @Override
    public void on(OrderMatchLog log) {
        OrderBook orderBook = orderBookByProductId.get(log.getProductId());
        if (orderBook == null) {
            orderBook = new OrderBook(log.getProductId(), null, null, new AtomicLong());
            orderBookByProductId.put(log.getProductId(),orderBook);
        }
        orderBook.restoreLog(log);
    }

    @Override
    public void on(OrderDoneMessage log) {
        OrderBook orderBook = orderBookByProductId.get(log.getProductId());
        if (orderBook == null) {
            orderBook = new OrderBook(log.getProductId(), null, null, new AtomicLong());
            orderBookByProductId.put(log.getProductId(),orderBook);
        }
        orderBook.restoreLog(log);
    }

    @Override
    public void on(OrderFilledMessage log) {

    }

    @Override
    public void on(AccountChangeMessage log) {

    }
}
