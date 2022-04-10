package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public abstract class OrderBookListener extends KafkaConsumerThread<String, OrderBookLog> implements OrderBookLogHandler {
    private final String productId;
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final OrderBookLogDispatcher messageDispatcher;
    private final AppProperties appProperties;
    private OrderBook orderBook;

    public OrderBookListener(String productId, OrderBookSnapshotManager orderBookSnapshotManager,
                             KafkaConsumer<String, OrderBookLog> kafkaConsumer, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.orderBookSnapshotManager = orderBookSnapshotManager;
        this.messageDispatcher = new OrderBookLogDispatcher(this);
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookLog> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        OrderBookSnapshot snapshot = orderBookSnapshotManager.getOrderBookSnapshot(productId);
                        orderBook = snapshot != null
                                ? snapshot.restore()
                                : new OrderBook(productId);

                        for (TopicPartition partition : partitions) {
                            if (snapshot != null) {
                                consumer.seek(partition, snapshot.getLogOffset() + 1);
                            }
                        }
                    }
                });
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderBookLog> consumer, ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog orderBookLog = record.value();
            orderBookLog.setOffset(record.offset());
            logger.info("- {} {} {}", record.offset(), orderBookLog.getSequence(), JSON.toJSONString(orderBookLog));

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
        PageLine line = orderBook.restoreLog(log);
        enqueuePageLine(line);
    }

    @Override
    public void on(OrderOpenLog log) {
        PageLine line = orderBook.restoreLog(log);
        enqueuePageLine(line);
    }

    @Override
    public void on(OrderMatchLog log) {
        PageLine line = orderBook.restoreLog(log);
        enqueuePageLine(line);
    }

    @Override
    public void on(OrderDoneLog log) {
        PageLine line = orderBook.restoreLog(log);
        enqueuePageLine(line);
    }

    protected abstract void onOrderBookChange(OrderBook orderBook, boolean stable, PageLine line) ;

    private void enqueuePageLine(PageLine line) {
        if (line != null) {
            onOrderBookChange(orderBook,true, line);
        }
    }

}
