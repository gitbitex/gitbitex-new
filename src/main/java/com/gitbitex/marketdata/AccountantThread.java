package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.AccountChangeLog;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.log.OrderRejectedLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class AccountantThread extends KafkaConsumerThread<String, Log>
    implements ConsumerRebalanceListener, LogHandler {
    private final AccountManager accountManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public AccountantThread(KafkaConsumer<String, Log> consumer,
        AccountManager accountManager,
        KafkaMessageProducer messageProducer,
        AppProperties appProperties) {
        super(consumer, logger);
        this.accountManager = accountManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
            consumer.commitSync();
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderBookLogTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, Log> record : records) {
            Log log = record.value();
            log.setOffset(record.offset());
            logger.info("{}", JSON.toJSONString(log));
            LogDispatcher.dispatch(log,this);
        }

        if (uncommittedRecordCount > 10) {
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }

    @Override
    public void on(OrderRejectedLog log) {

    }

    @Override
    public void on(OrderReceivedLog log) {

    }

    @Override
    public void on(OrderOpenLog log) {

    }

    @Override
    public void on(OrderMatchLog log) {

    }

    @Override
    public void on(OrderDoneLog log) {

    }

    @Override
    public void on(OrderFilledMessage log) {

    }

    @Override
    public void on(AccountChangeLog message) {
        accountManager.deposit(message);
    }

}



