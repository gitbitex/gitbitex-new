package com.gitbitex.marketdata;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

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
        consumer.poll(Duration.ofSeconds(5)).forEach(x -> {
            Log log = x.value();
            log.setOffset(x.offset());
            LogDispatcher.dispatch(log, this);
        });
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
    public void on(AccountChangeLog log) {
        logger.info("{}", JSON.toJSONString(log));
        accountManager.deposit(log);
    }
}



