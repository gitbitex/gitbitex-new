package com.gitbitex.matchingengine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.Command;
import com.gitbitex.matchingengine.command.CommandDispatcher;
import com.gitbitex.matchingengine.command.CommandHandler;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RedissonClient;

@Slf4j
public class MatchingEngineThread extends KafkaConsumerThread<String, Command>
    implements CommandHandler, ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final MatchingEngineStateStore matchingEngineStateStore;
    KafkaMessageProducer producer;
    RedissonClient redissonClient;
    OrderBookManager orderBookManager;
    long t1;
    long t2;
    int i;
    private MatchingEngine matchingEngine;

    public MatchingEngineThread(KafkaConsumer<String, Command> messageKafkaConsumer,
        MatchingEngineStateStore matchingEngineStateStore,
        KafkaMessageProducer producer, RedissonClient redissonClient, OrderBookManager orderBookManager,
        AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.appProperties = appProperties;
        this.matchingEngineStateStore = matchingEngineStateStore;
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.orderBookManager = orderBookManager;
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
            matchingEngine = new MatchingEngine(matchingEngineStateStore, producer, redissonClient,
                orderBookManager, appProperties);
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
        if (t1 == 0) {
            t1 = System.currentTimeMillis();
        }
        consumer.poll(Duration.ofSeconds(5)).forEach(x -> {
            Command command = x.value();
            command.setOffset(x.offset());
            //logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);
            i++;
            /*try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
        });

        //logger.info(i + " " + (System.currentTimeMillis() - t1));
        //System.out.println(i + " " + (System.currentTimeMillis() - t1));

        //matchingEngine.getOrderBooks().keySet().forEach(x -> {
        //L2OrderBook l2OrderBook = matchingEngine.takeL2OrderBookSnapshot(x, 10);
        //logger.info(JSON.toJSONString(l2OrderBook, true));
        //orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        //});
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
