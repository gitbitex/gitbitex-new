package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, MatchingEngineCommand>
        implements MatchingEngineCommandHandler, ConsumerRebalanceListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final LogWriter logWriter;
    private MatchingEngine matchingEngine;

    public MatchingThread(OrderBookManager orderBookManager,
                          KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer, KafkaMessageProducer messageProducer,
                          RedissonClient redissonClient, AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
        this.logWriter = new LogWriter(messageProducer, redissonClient, appProperties);
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
            MatchingEngineSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot();
            this.matchingEngine = new MatchingEngine(snapshot, logWriter);
            if (snapshot != null) {
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
            //logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //System.out.println(i);

        //MatchingEngineSnapshot snapshot = matchingEngine.takeSnapshot();
        //logger.info(JSON.toJSONString(snapshot, true));
        //orderBookManager.saveFullOrderBookSnapshot(snapshot);

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
