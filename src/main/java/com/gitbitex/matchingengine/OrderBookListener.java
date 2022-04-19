package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.snapshot.FullOrderBookSnapshot;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class OrderBookListener extends KafkaConsumerThread<String, OrderBookLog> implements OrderBookLogHandler, ConsumerRebalanceListener {
    protected final Map<String, OrderBook> orderBookByProductId = new HashMap<>();
    private final List<String> productIds;
    private final OrderBookManager orderBookManager;
    private final OrderBookLogDispatcher messageDispatcher;
    private final AppProperties appProperties;
    private final Duration pollTimeout;

    public OrderBookListener(List<String> productIds, OrderBookManager orderBookManager,
                             KafkaConsumer<String, OrderBookLog> kafkaConsumer,
                             Duration pollTimeout,
                             AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productIds = productIds;
        this.orderBookManager = orderBookManager;
        this.messageDispatcher = new OrderBookLogDispatcher(this);
        this.appProperties = appProperties;
        this.pollTimeout = pollTimeout;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            orderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            OrderBook orderBook;
            FullOrderBookSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot(productId);
            if (snapshot != null) {
                orderBook = snapshot.decodeOrderBook();
                consumer.seek(partition, orderBook.getLogOffset() + 1);
            } else {
                orderBook = new OrderBook(productId);
            }
            orderBookByProductId.put(productId, orderBook);
        }
    }

    @Override
    protected void doSubscribe() {
        List<String> topics = productIds.stream()
                .map(x -> TopicUtil.getProductTopic(x, appProperties.getOrderBookLogTopic()))
                .collect(Collectors.toList());
        consumer.subscribe(topics, this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(pollTimeout);

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            log.setOffset(record.offset());

            // check the sequence to ensure that each message is processed in order
            OrderBook orderBook = orderBookByProductId.get(log.getProductId());
            if (log.getSequence() <= orderBook.getSequence().get()) {
                logger.warn("discard log sequence= {}", log.getSequence());
                continue;
            } else if (orderBook.getSequence().get() + 1 != log.getSequence()) {
                throw new RuntimeException("unexpected sequence");
            }

            messageDispatcher.dispatch(log);

            afterRecordProcessed(log.getProductId());
        }

        afterRecordsProcessed(records.count());
    }

    protected void afterRecordProcessed(String productId) {
    }

    protected void afterRecordsProcessed(int recordCount) {
    }

    @Override
    public void on(OrderReceivedLog log) {
        orderBookByProductId.get(log.getProductId()).restoreLog(log);
    }

    @Override
    public void on(OrderOpenLog log) {
        orderBookByProductId.get(log.getProductId()).restoreLog(log);
    }

    @Override
    public void on(OrderMatchLog log) {
        orderBookByProductId.get(log.getProductId()).restoreLog(log);
    }

    @Override
    public void on(OrderDoneLog log) {
        orderBookByProductId.get(log.getProductId()).restoreLog(log);
    }

}
