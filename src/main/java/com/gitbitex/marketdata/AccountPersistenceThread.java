package com.gitbitex.marketdata;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.AccountEntity;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.matchingengine.Account;
import com.gitbitex.matchingengine.message.AccountMessage;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AccountPersistenceThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    private final AccountManager accountManager;
    private final AppProperties appProperties;
    private final RTopic accountTopic;

    public AccountPersistenceThread(KafkaConsumer<String, Message> consumer, AccountManager accountManager,
                                    RedissonClient redissonClient,
                                    AppProperties appProperties) {
        super(consumer, logger);
        this.accountManager = accountManager;
        this.appProperties = appProperties;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {

    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        Map<String, AccountEntity> accounts = new HashMap<>();
        records.forEach(x -> {
            Message message = x.value();
            if (message instanceof AccountMessage accountMessage) {
                AccountEntity accountEntity = accountEntity(accountMessage);
                accounts.put(accountEntity.getId(), accountEntity);
                accountTopic.publishAsync(JSON.toJSONString(accountMessage));
            }
        });
        accountManager.saveAll(accounts.values());

        consumer.commitAsync();
    }

    private AccountEntity accountEntity(AccountMessage message) {
        Account account = message.getAccount();
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setId(account.getUserId() + "-" + account.getCurrency());
        accountEntity.setUserId(account.getUserId());
        accountEntity.setCurrency(account.getCurrency());
        accountEntity.setAvailable(account.getAvailable());
        accountEntity.setHold(account.getHold());
        return accountEntity;
    }
}



