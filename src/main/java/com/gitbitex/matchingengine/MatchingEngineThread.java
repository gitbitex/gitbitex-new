package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MatchingEngineThread extends KafkaConsumerThread<String, Command>
        implements CommandHandler, ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final EngineSnapshotStore engineSnapshotStore;
    private final List<EngineListener> engineListeners;
    private MatchingEngine matchingEngine;

    public MatchingEngineThread(KafkaConsumer<String, Command> consumer, EngineSnapshotStore engineSnapshotStore,
                                List<EngineListener> engineListeners, AppProperties appProperties) {
        super(consumer, logger);
        this.appProperties = appProperties;
        this.engineSnapshotStore = engineSnapshotStore;
        this.engineListeners = engineListeners;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            matchingEngine = new MatchingEngine(engineSnapshotStore, engineListeners);
            if (matchingEngine.getStartupCommandOffset() != null) {
                logger.info("seek to offset: {}", matchingEngine.getStartupCommandOffset() + 1);
                consumer.seek(partition, matchingEngine.getStartupCommandOffset() + 1);
            }
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineCommandTopic()), this);
    }

    @Override
    protected void doPoll() {
        ConsumerRecords<String, Command> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }
        records.forEach(x -> {
            Command command = x.value();
            command.setOffset(x.offset());
            //logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);
            /*try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
        });
    }

    @Override
    public void on(PutProductCommand command) {
        matchingEngine.executeCommand(command);
    }

    @Override
    public void on(DepositCommand command) {
        matchingEngine.executeCommand(command);
    }

    @Override
    public void on(PlaceOrderCommand command) {
        matchingEngine.executeCommand(command);
    }

    @Override
    public void on(CancelOrderCommand command) {
        matchingEngine.executeCommand(command);
    }

}
