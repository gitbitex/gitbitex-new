package com.gitbitex.matchingengine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.CommandDispatcher;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandHandler;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, MatchingEngineCommand>
    implements MatchingEngineCommandHandler, ConsumerRebalanceListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final LogWriter logWriter;
    private MatchingEngine matchingEngine;

    public MatchingThread(OrderBookManager orderBookManager,
        KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer, KafkaMessageProducer messageProducer,
        AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
        this.logWriter = new LogWriter(messageProducer);
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
            EngineSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot();
            this.matchingEngine = new MatchingEngine(snapshot, logWriter);

            if (snapshot != null) {
                consumer.seek(partition, snapshot.getCommandOffset() + 1);
            }
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getOrderBookCommandTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, MatchingEngineCommand> record : records) {
            MatchingEngineCommand command = record.value();
            command.setOffset(record.offset());
            logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);
        }

        EngineSnapshot snapshot = matchingEngine.takeSnapshot();
        //logger.info(JSON.toJSONString(snapshot, true));
        orderBookManager.saveFullOrderBookSnapshot(snapshot);

        matchingEngine.getOrderBooks().keySet().forEach(x -> {
            L2OrderBook l2OrderBook = matchingEngine.takeL2OrderBookSnapshot(x, 10);
            //logger.info(JSON.toJSONString(l2OrderBook, true));
            orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        });

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
