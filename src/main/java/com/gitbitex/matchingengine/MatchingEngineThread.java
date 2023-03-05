package com.gitbitex.matchingengine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.CommandDispatcher;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandHandler;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class MatchingEngineThread extends KafkaConsumerThread<String, MatchingEngineCommand>
    implements MatchingEngineCommandHandler, ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final DirtyObjectHandler dirtyObjectHandler;
    protected MatchingEngine matchingEngine;
    protected long lastSnapshotOffset;
    protected long lastSnapshotTime;
    protected long offset;
    private final MatchingEngineStateStore matchingEngineStateStore;

    public MatchingEngineThread(KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer, MatchingEngineStateStore matchingEngineStateStore, DirtyObjectHandler dirtyObjectHandler,
        AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.appProperties = appProperties;
        this.dirtyObjectHandler = dirtyObjectHandler;
        this.matchingEngineStateStore=matchingEngineStateStore;
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
            MatchingEngineSnapshot snapshot = null;// = orderBookManager.getFullOrderBookSnapshot();
            this.matchingEngine = new MatchingEngine(matchingEngineStateStore, dirtyObjectHandler);
            if (snapshot != null) {
                offset = snapshot.getCommandOffset();
                lastSnapshotOffset = snapshot.getCommandOffset();
                lastSnapshotTime = snapshot.getTime();
                consumer.seek(partition, snapshot.getCommandOffset() + 1);
            }
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineCommandTopic()), this);
    }

    @Override
    protected void doPoll() {
        consumer.poll(Duration.ofSeconds(5)).forEach(x -> {
            MatchingEngineCommand command = x.value();
            command.setOffset(x.offset());
            offset = x.offset();
            //logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //matchingEngine.getOrderBooks().keySet().forEach(x -> {
        //L2OrderBook l2OrderBook = matchingEngine.takeL2OrderBookSnapshot(x, 10);
        //logger.info(JSON.toJSONString(l2OrderBook, true));
        //orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        //});
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
