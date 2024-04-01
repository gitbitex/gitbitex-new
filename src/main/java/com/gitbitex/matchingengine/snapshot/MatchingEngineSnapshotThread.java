package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.*;
import com.gitbitex.matchingengine.message.*;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MatchingEngineSnapshotThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    private final EngineSnapshotManager engineSnapshotManager;
    private final AppProperties appProperties;
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, Product> products = new HashMap<>();
    private EngineState engineState;

    public MatchingEngineSnapshotThread(KafkaConsumer<String, Message> consumer,
                                        EngineSnapshotManager engineSnapshotManager, AppProperties appProperties) {
        super(consumer, logger);
        this.engineSnapshotManager = engineSnapshotManager;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        engineSnapshotManager.runInSession(session -> {
            engineState = engineSnapshotManager.getEngineState(session);
            if (engineState == null) {
                engineState = new EngineState();
            }
        });
        cleanBuffers();

        if (engineState.getMessageOffset() != null) {
            long offset = engineState.getMessageOffset() + 1;
            logger.info("seek to offset: {}", offset);
            consumer.seek(partitions.iterator().next(), offset);
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, Message> record : records) {
            Message message = record.value();

            long expectedSequence = engineState.getMessageSequence() != null
                    ? engineState.getMessageSequence() + 1 : 1;
            if (message.getSequence() < expectedSequence) {
                continue;
            } else if (message.getSequence() > expectedSequence) {
                throw new RuntimeException(String.format("out of sequence: sequence=%s, expectedSequence=%s", message.getSequence(), expectedSequence));
            }

            engineState.setMessageOffset(record.offset());
            engineState.setMessageSequence(message.getSequence());

            if (message instanceof OrderMessage orderMessage) {
                Order order = orderMessage.getOrder();
                orders.put(order.getId(), order);
                engineState.getOrderSequences().put(order.getProductId(), order.getSequence());
                engineState.getOrderBookSequences().put(order.getProductId(), orderMessage.getOrderBookSequence());

            } else if (message instanceof TradeMessage tradeMessage) {
                Trade trade = tradeMessage.getTrade();
                engineState.getTradeSequences().put(trade.getProductId(), trade.getSequence());

            } else if (message instanceof AccountMessage accountMessage) {
                Account account = accountMessage.getAccount();
                accounts.put(account.getId(), account);

            } else if (message instanceof ProductMessage productMessage) {
                Product product = productMessage.getProduct();
                products.put(product.getId(), product);

            } else if (message instanceof CommandStartMessage commandStartMessage) {
                engineState.setCommandOffset(null);

            } else if (message instanceof CommandEndMessage commandEndMessage) {
                engineState.setCommandOffset(commandEndMessage.getCommandOffset());

                saveState();
            }
        }
    }

    private void saveState() {
        engineSnapshotManager.save(engineState, accounts.values(), orders.values(), products.values());
        cleanBuffers();
    }

    private void cleanBuffers() {
        accounts.clear();
        orders.clear();
        products.clear();
    }

}
