package com.gitbitex.matchingengine;

import com.codahale.metrics.MetricRegistry;
import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.snapshot.FullOrderBookSnapshot;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, OrderBookCommand> implements OrderBookCommandHandler, ConsumerRebalanceListener {
    private final List<String> productIds;
    private final OrderBookManager orderBookManager;
    private final OrderBookCommandDispatcher orderBookCommandDispatcher;
    private final AppProperties appProperties;
    private final KafkaMessageProducer messageProducer;
    private final MetricRegistry metricRegistry;
    private final Map<String, OrderBook> orderBookByProductId = new HashMap<>();

    public MatchingThread(List<String> productIds,
                          OrderBookManager orderBookManager,
                          KafkaConsumer<String, OrderBookCommand> messageKafkaConsumer,
                          KafkaMessageProducer messageProducer,
                          MetricRegistry metricRegistry,
                          AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.productIds = productIds;
        this.orderBookManager = orderBookManager;
        this.orderBookCommandDispatcher = new OrderBookCommandDispatcher(this);
        this.appProperties = appProperties;
        this.messageProducer = messageProducer;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.warn("partition revoked: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            orderBookByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            OrderBook orderBook;
            FullOrderBookSnapshot snapshot = orderBookManager.getFullOrderBookSnapshot(productId);
            if (snapshot != null) {
                orderBook = snapshot.decodeOrderBook();
                consumer.seek(partition, orderBook.getCommandOffset() + 1);
            } else {
                orderBook = new OrderBook(productId);
            }
            orderBookByProductId.put(productId, orderBook);
        }
    }

    @Override
    protected void doSubscribe() {
        List<String> topics = productIds.stream()
                .map(x -> TopicUtil.getProductTopic(x, appProperties.getOrderBookCommandTopic()))
                .collect(Collectors.toList());
        consumer.subscribe(topics, this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        for (ConsumerRecord<String, OrderBookCommand> record : records) {
            OrderBookCommand command = record.value();
            command.setOffset(record.offset());
            orderBookCommandDispatcher.dispatch(command);

            metricRegistry.meter("orderBookCommand.processed.total").mark();
        }
    }

    @Override
    public void on(NewOrderCommand command) {
        List<OrderBookLog> logs = orderBookByProductId.get(command.getProductId()).executeCommand(command);
        if (logs != null) {
            for (OrderBookLog log : logs) {
                sendOrderBookLog(log);
            }
        }
    }

    @Override
    public void on(CancelOrderCommand command) {
        OrderBookLog log = orderBookByProductId.get(command.getProductId()).executeCommand(command);
        if (log != null) {
            sendOrderBookLog(log);
        }
    }

    private void sendOrderBookLog(OrderBookLog log) {
        messageProducer.sendOrderBookLog(log, (metadata, e) -> {
            if (e != null) {
                logger.error("send order book log error: {}", e.getMessage(), e);
                this.shutdown();
            }
        });
    }

}
