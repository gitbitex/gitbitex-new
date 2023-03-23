package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.matchingengine.message.AccountMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AccountPersistenceThread extends KafkaConsumerThread<String, AccountMessage>
        implements ConsumerRebalanceListener {
    private final AccountManager accountManager;
    private final AppProperties appProperties;

    public AccountPersistenceThread(KafkaConsumer<String, AccountMessage> consumer, AccountManager accountManager,
                                    AppProperties appProperties) {
        super(consumer, logger);
        this.accountManager = accountManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
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
        var records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, Account> accounts = new HashMap<>();
        records.forEach(x -> {
            Account account = account(x.value());
            accounts.put(account.getId(), account);
        });

        long t1 = System.currentTimeMillis();
        accountManager.saveAll(accounts.values());
        logger.info("saved {} account(s) ({}ms)", accounts.size(), System.currentTimeMillis() - t1);

        consumer.commitSync();
    }

    private Account account(AccountMessage message) {
        Account account = new Account();
        account.setId(message.getUserId() + "-" + message.getCurrency());
        account.setUserId(message.getUserId());
        account.setCurrency(message.getCurrency());
        account.setAvailable(message.getAvailable());
        account.setHold(message.getHold());
        return account;
    }

}



