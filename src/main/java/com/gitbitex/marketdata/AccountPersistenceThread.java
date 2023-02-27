package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.matchingengine.log.LogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.log.OrderRejectedLog;
import com.gitbitex.matchingengine.log.TradeMessage;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class AccountPersistenceThread extends KafkaConsumerThread<String, Log>
    implements ConsumerRebalanceListener, LogHandler {
    private final AccountManager accountManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public AccountPersistenceThread(KafkaConsumer<String, Log> consumer,
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
        consumer.subscribe(Collections.singletonList(appProperties.getAccountMessageTopic()), this);
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
    public void on(AccountMessage log) {
        logger.info("{}", JSON.toJSONString(log));

        Account account = new Account();
        account.setId(log.getUserId() + "-" + log.getCurrency());
        account.setUserId(log.getUserId());
        account.setCurrency(log.getCurrency());
        account.setAvailable(log.getAvailable());
        account.setHold(log.getHold());
        accountManager.save(account);
    }

    @Override
    public void on(OrderMessage message) {

    }

    @Override
    public void on(TradeMessage message) {

    }
}



