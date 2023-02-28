package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class AccountPersistenceThread extends KafkaConsumerThread<String, AccountMessage>
    implements ConsumerRebalanceListener {
    private final AccountManager accountManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public AccountPersistenceThread(KafkaConsumer<String, AccountMessage> consumer,
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
            //consumer.commitSync();
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
        ConsumerRecords<String, AccountMessage> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, Account> orders = new HashMap<>();
        records.forEach(x -> {
            Account account = account(x.value());
            orders.put(account.getId(), account);
        });

        long t1 = System.currentTimeMillis();
        accountManager.saveAll(orders.values());
        long t2 = System.currentTimeMillis();
        logger.info("account size: {} time: {}", orders.size(), t2 - t1);
    }

    private Account account(AccountMessage log) {
        Account account = new Account();
        account.setId(log.getUserId() + "-" + log.getCurrency());
        account.setUserId(log.getUserId());
        account.setCurrency(log.getCurrency());
        account.setAvailable(log.getAvailable());
        account.setHold(log.getHold());
        return account;
    }

}



