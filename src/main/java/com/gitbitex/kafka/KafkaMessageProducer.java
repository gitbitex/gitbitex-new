package com.gitbitex.kafka;

import com.alibaba.fastjson.JSON;
import com.codahale.metrics.MetricRegistry;
import com.gitbitex.AppProperties;
import com.gitbitex.account.command.AccountCommand;
import com.gitbitex.matchingengine.command.OrderBookCommand;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.order.command.OrderCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.Future;

@Slf4j
public class KafkaMessageProducer extends KafkaProducer<String, String> {
    private final AppProperties appProperties;
    private final MetricRegistry metricRegistry;

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties, MetricRegistry metricRegistry) {
        super(kafkaProperties);
        this.appProperties = appProperties;
        this.metricRegistry = metricRegistry;
    }

    public Future<RecordMetadata> sendAccountCommand(AccountCommand command, Callback callback) {
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }

        String topic = appProperties.getAccountCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getUserId(),
                JSON.toJSONString(command));

        metricRegistry.meter("accountCommand.new.total").mark();
        return super.send(record, (metadata, exception) -> {
            if (exception == null) {
                metricRegistry.meter("accountCommand.sent.total").mark();
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendOrderCommand(OrderCommand command, Callback callback) {
        if (command.getOrderId() == null) {
            throw new NullPointerException("orderId");
        }

        String topic = appProperties.getOrderCommandTopic();
        String key = command.getOrderId();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, JSON.toJSONString(command));

        metricRegistry.meter("orderCommand.new.total").mark();
        return super.send(record, (metadata, exception) -> {
            if (exception == null) {
                metricRegistry.meter("orderCommand.sent.total").mark();
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendOrderBookLog(OrderBookLog log, Callback callback) {
        if (log.getProductId() == null) {
            throw new NullPointerException("productId");
        }

        String topic = TopicUtil.getProductTopic(log.getProductId(), appProperties.getOrderBookLogTopic());
        String key = log.getProductId();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, JSON.toJSONString(log));

        metricRegistry.meter("orderBookLog.new.total").mark();
        return super.send(record, (metadata, exception) -> {
            if (exception == null) {
                metricRegistry.meter("orderBookLog.sent.total").mark();
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendOrderBookCommand(OrderBookCommand command, Callback callback) {
        if (command.getProductId() == null) {
            throw new NullPointerException("productId");
        }

        String topic = TopicUtil.getProductTopic(command.getProductId(), appProperties.getOrderBookCommandTopic());
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getProductId(),
                JSON.toJSONString(command));

        metricRegistry.meter("orderBookCommand.new.total").mark();
        return super.send(record, (metadata, exception) -> {
            if (exception == null) {
                metricRegistry.meter("orderBookCommand.sent.total").mark();
            }

            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }
}
