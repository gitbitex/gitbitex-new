package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, MatchingEngineCommand>
        implements MatchingEngineCommandHandler, ConsumerRebalanceListener {
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final LogWriter logWriter;
    private MatchingEngine matchingEngine;

    public MatchingThread(OrderBookManager orderBookManager,
                          KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer, KafkaMessageProducer messageProducer,
                          RedissonClient redissonClient,
                          AppProperties appProperties) {
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

    long i = 0;
    long t1;
    long t2;

    BlockingQueue<MatchingEngineCommand> commands = new LinkedBlockingQueue<>();
    ExecutorService executorService = Executors.newFixedThreadPool(1);

    List<MatchingEngineCommand> commandList = new ArrayList<>();

    @Override
    protected void doPoll() {

        if (i == 0) t1 = System.currentTimeMillis();

        consumer.poll(Duration.ofSeconds(5)).forEach(x -> {
            MatchingEngineCommand command = x.value();
            command.setOffset(x.offset());
            //logger.info("{}", JSON.toJSONString(command));
            CommandDispatcher.dispatch(command, this);
            //commands.offer(command);
            //commandList.add(command);
            i++;
            //System.out.println(i);
        });

        /*if (i>100000){
            System.out.println("staring");
            t1=System.currentTimeMillis();
            for (MatchingEngineCommand matchingEngineCommand : commandList) {
                System.out.println("command-"+matchingEngineCommand.getOffset());
                CommandDispatcher.dispatch(matchingEngineCommand, this);
            }
            t2 = System.currentTimeMillis();
            System.out.println(t2-t1);
            System.exit(1);
        }*/


        //if (i==21410) {
        t2 = System.currentTimeMillis();
        System.out.println("i=" + i + " time=" + (t2 - t1));
        //}

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
