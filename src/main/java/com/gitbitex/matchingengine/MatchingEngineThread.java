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

@Slf4j
public class MatchingEngineThread extends KafkaConsumerThread<String, Command>
        implements CommandHandler, ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final EngineStateStore engineStateStore;
    private final ModifiedObjectWriter modifiedObjectWriter;
    private final EngineStateWriter engineStateWriter;
    private final OrderBookSnapshotPublisher orderBookSnapshotPublisher;
    private MatchingEngine matchingEngine;

    public MatchingEngineThread(KafkaConsumer<String, Command> consumer, EngineStateStore engineStateStore,
                                ModifiedObjectWriter modifiedObjectWriter, EngineStateWriter engineStateWriter,
                                OrderBookSnapshotPublisher orderBookSnapshotPublisher, AppProperties appProperties) {
        super(consumer, logger);
        this.appProperties = appProperties;
        this.engineStateStore = engineStateStore;
        this.modifiedObjectWriter = modifiedObjectWriter;
        this.engineStateWriter = engineStateWriter;
        this.orderBookSnapshotPublisher = orderBookSnapshotPublisher;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
        }
        if (matchingEngine != null) {
            matchingEngine.shutdown();
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            matchingEngine = new MatchingEngine(engineStateStore, modifiedObjectWriter, engineStateWriter,
                    orderBookSnapshotPublisher);
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
