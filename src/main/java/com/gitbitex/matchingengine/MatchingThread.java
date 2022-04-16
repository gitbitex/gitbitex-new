package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MatchingThread extends KafkaConsumerThread<String, OrderBookCommand> implements OrderBookCommandHandler {
    private final String productId;
    private final OrderBookManager orderBookManager;
    private final OrderBookCommandDispatcher orderBookCommandDispatcher;
    private final AppProperties appProperties;
    private final KafkaMessageProducer messageProducer;
    private OrderBook orderBook;

    public MatchingThread(String productId, OrderBookManager orderBookManager,
                          KafkaConsumer<String, OrderBookCommand> messageKafkaConsumer,
                          KafkaMessageProducer messageProducer,
                          AppProperties appProperties) {
        super(messageKafkaConsumer, logger);
        this.productId = productId;
        this.orderBookManager = orderBookManager;
        this.orderBookCommandDispatcher = new OrderBookCommandDispatcher(this);
        this.appProperties = appProperties;
        this.messageProducer = messageProducer;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookCommandTopic()),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        for (TopicPartition partition : partitions) {
                            logger.warn("partition revoked: {}", partition.toString());
                        }
                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        orderBook = orderBookManager.getOrderBook(productId);
                        if (orderBook != null) {
                            for (TopicPartition partition : partitions) {
                                consumer.seek(partition, orderBook.getCommandOffset() + 1);
                            }
                        } else {
                            orderBook = new OrderBook(productId);
                        }
                    }
                });
    }

    @Override
    protected void processRecords(ConsumerRecords<String, OrderBookCommand> records) {
        for (ConsumerRecord<String, OrderBookCommand> record : records) {
            OrderBookCommand command = record.value();
            command.setOffset(record.offset());
            this.orderBookCommandDispatcher.dispatch(command);
        }
    }

    @Override
    @SneakyThrows
    public void on(NewOrderCommand command) {
        List<OrderBookLog> logs = orderBook.executeCommand(command);
        if (logs != null) {
            for (OrderBookLog log : logs) {
                sendOrderBookLog(log);
            }
        }
    }

    @Override
    @SneakyThrows
    public void on(CancelOrderCommand command) {
        OrderBookLog log = orderBook.executeCommand(command);
        if (log != null) {
            sendOrderBookLog(log);
        }
    }

    public void sendOrderBookLog(OrderBookLog log) {
        messageProducer.sendOrderBookLog(log, (metadata, e) -> {
            if (e != null) {
                logger.error("send order book log error: {}", e.getMessage(), e);
                this.shutdown();
            }
        });
    }
}
