package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.AccountEntity;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.matchingengine.MessageConsumerThread;
import com.gitbitex.matchingengine.message.AccountMessage;
import com.gitbitex.matchingengine.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AccountPersistenceThread extends MessageConsumerThread {
    private final AccountManager accountManager;
    private final AppProperties appProperties;

    public AccountPersistenceThread(KafkaConsumer<String, Message> consumer, AccountManager accountManager,
                                    AppProperties appProperties) {
        super(consumer, appProperties, logger);
        this.accountManager = accountManager;
        this.appProperties = appProperties;
    }

    @Override
    protected void processRecords(ConsumerRecords<String, Message> records) {
        Map<String, AccountEntity> accounts = new HashMap<>();
        for (ConsumerRecord<String, Message> record : records) {
            Message message = record.value();
            if (message instanceof AccountMessage accountMessage) {
                AccountEntity account = account(accountMessage);
                accounts.put(account.getId(), account);
            }
        }
        accountManager.saveAll(accounts.values());
    }

    private AccountEntity account(AccountMessage message) {
        AccountEntity account = new AccountEntity();
        account.setId(message.getAccount().getUserId() + "-" + message.getAccount().getCurrency());
        account.setUserId(message.getAccount().getUserId());
        account.setCurrency(message.getAccount().getCurrency());
        account.setAvailable(message.getAccount().getAvailable());
        account.setHold(message.getAccount().getHold());
        return account;
    }

}



