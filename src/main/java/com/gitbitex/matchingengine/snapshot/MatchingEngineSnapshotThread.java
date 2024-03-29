package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.*;
import com.gitbitex.matchingengine.message.*;
import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import com.gitbitex.matchingengine.snapshot.EngineState;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MatchingEngineSnapshotThread extends MessageConsumerThread {
    private final EngineSnapshotManager snapshotStore;
    private final AppProperties appProperties;
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, Product> products = new HashMap<>();
    private Long lastSnapshotCommandOffset;
    private EngineState lastEngineState;

    public MatchingEngineSnapshotThread(KafkaConsumer<String, Message> consumer, EngineSnapshotManager engineSnapshotManager, AppProperties appProperties) {
        super(consumer, appProperties, logger);
        this.snapshotStore = engineSnapshotManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        super.onPartitionsAssigned(partitions);

        snapshotStore.runInSession(session -> {
            EngineState engineState = snapshotStore.getEngineState(session);
            if (engineState != null) {
                lastEngineState = engineState;
                lastSnapshotCommandOffset = engineState.getCommandOffset();
            } else {
                lastEngineState = new EngineState();
            }
        });
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void processRecords(ConsumerRecords<String, Message> records) {
        for (ConsumerRecord<String, Message> record : records) {
            Message message = record.value();

            checkMessageSequence(new TopicPartition(record.topic(), record.partition()), message);

            lastEngineState.setMessageOffset(record.offset());
            lastEngineState.setMessageSequence(message.getSequence());

            if (message instanceof OrderMessage orderMessage) {
                Order order = orderMessage.getOrder();
                orders.put(order.getId(), order);
                lastEngineState.getOrderSequences().put(order.getProductId(), order.getSequence());
                lastEngineState.getOrderBookSequences().put(order.getProductId(), orderMessage.getOrderBookSequence());

            } else if (message instanceof TradeMessage tradeMessage) {
                Trade trade = tradeMessage.getTrade();
                lastEngineState.getTradeSequences().put(trade.getProductId(), trade.getSequence());

            } else if (message instanceof AccountMessage accountMessage) {
                Account account = accountMessage.getAccount();
                accounts.put(account.getId(), account);

            } else if (message instanceof ProductMessage productMessage) {
                Product product = productMessage.getProduct();
                products.put(product.getId(), product);

            } else if (message instanceof CommandStartMessage commandStartMessage) {
                lastEngineState.setCommandOffset(null);

            } else if (message instanceof CommandEndMessage commandEndMessage) {
                lastEngineState.setCommandOffset(commandEndMessage.getCommandOffset());

                saveState();
            }
        }
    }

    private void saveState() {
        snapshotStore.save(lastEngineState, accounts.values(), orders.values(), products.values());
        lastSnapshotCommandOffset = lastEngineState.getCommandOffset();

        accounts.clear();
        orders.clear();
        products.clear();
    }

}
