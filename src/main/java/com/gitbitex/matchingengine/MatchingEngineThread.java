package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class MatchingEngineThread extends KafkaConsumerThread<String, Command>
        implements ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final EngineSnapshotManager engineSnapshotManager;
    private final MessageSender messageSender;
    private MatchingEngine matchingEngine;

    public MatchingEngineThread(KafkaConsumer<String, Command> consumer, EngineSnapshotManager engineSnapshotManager, MessageSender messageSender,
                                AppProperties appProperties) {
        super(consumer, logger);
        this.appProperties = appProperties;
        this.engineSnapshotManager = engineSnapshotManager;
        this.messageSender = messageSender;
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
            matchingEngine = new MatchingEngine(engineSnapshotManager, messageSender);
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
        consumer.poll(Duration.ofSeconds(5))
                .forEach(x -> matchingEngine.executeCommand(x.value(), x.offset()));
    }
}
