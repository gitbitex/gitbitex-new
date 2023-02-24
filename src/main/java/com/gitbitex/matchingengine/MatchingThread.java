package com.gitbitex.matchingengine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.CommandDispatcher;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandHandler;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
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
    private final List<String> productIds;
    private final OrderBookManager orderBookManager;
    private final AppProperties appProperties;
    private final KafkaMessageProducer messageProducer;
    private final LogWriter logWriter;
    private final Map<String, OrderBook> orderBookByProductId = new HashMap<>();
    private final AccountBook accountBook;
    private final AtomicLong sequence = new AtomicLong();
    private MatchingEngine matchingEngine;

    public MatchingThread(List<String> productIds, OrderBookManager orderBookManager,
        KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer, KafkaMessageProducer messageProducer,
        AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.productIds = productIds;
        this.orderBookManager = orderBookManager;
        this.appProperties = appProperties;
        this.messageProducer = messageProducer;
        this.logWriter = new LogWriter(messageProducer);
        this.accountBook = new AccountBook(logWriter, sequence);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
            //String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            //orderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());

            /*FullOrderBookSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot("productId");
            if (snapshot != null) {
                orderBook = new OrderBook(snapshot, engineLogger);
                consumer.seek(partition, orderBook.getCommandOffset() + 1);
            } else {
                orderBook = new OrderBook(productId, engineLogger, new AccountBook());
            }
            orderBookByProductId.put(productId, orderBook);*/
        }

        this.matchingEngine = new MatchingEngine(null, logWriter);
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

        EngineSnapshot snapshot= matchingEngine.takeSnapshot();
        logger.info(JSON.toJSONString(snapshot,true));
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
