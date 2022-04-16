package com.gitbitex.kafka;

import com.alibaba.fastjson.JSON;
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

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties) {
        super(kafkaProperties);
        this.appProperties = appProperties;
    }

    public Future<RecordMetadata> sendOrderBookCommand(OrderBookCommand command, Callback callback) {
        if (command.getProductId() == null) {
            throw new NullPointerException("productId");
        }

        String topic = command.getProductId() + "-" + appProperties.getOrderBookCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getProductId(),
                JSON.toJSONString(command));
        return super.send(record, callback);
    }

    public Future<RecordMetadata> sendAccountCommand(AccountCommand command, Callback callback) {
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }

        String topic = appProperties.getAccountCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getUserId(),
                JSON.toJSONString(command));
        return super.send(record, callback);
    }

    public Future<RecordMetadata> sendOrderCommand(OrderCommand command, Callback callback) {
        if (command.getOrderId() == null) {
            throw new NullPointerException("bad OrderCommand: orderId is null");
        }

        String topic = appProperties.getOrderCommandTopic();
        String key = command.getOrderId();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, JSON.toJSONString(command));
        return super.send(record, callback);
    }

    public Future<RecordMetadata> sendOrderBookLog(OrderBookLog log, Callback callback) {
        if (log.getProductId() == null) {
            throw new NullPointerException("bad OrderBookLog: productId is null");
        }

        String topic = log.getProductId() + "-" + appProperties.getOrderBookLogTopic();
        String key = log.getProductId();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, JSON.toJSONString(log));
        return super.send(record, callback);
    }
}
