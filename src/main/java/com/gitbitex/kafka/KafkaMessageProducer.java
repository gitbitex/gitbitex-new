package com.gitbitex.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.account.command.AccountCommand;
import com.gitbitex.matchingengine.command.OrderBookCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
public class KafkaMessageProducer extends KafkaProducer<String, String> {
    private final AppProperties appProperties;

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties) {
        super(kafkaProperties);
        this.appProperties = appProperties;
    }

    @SneakyThrows
    public void sendToMatchingEngine(OrderBookCommand command) {
        long t1=System.currentTimeMillis();
        if (command.getProductId() == null) {
            throw new NullPointerException("productId");
        }

        String topic = command.getProductId() + "-" + appProperties.getOrderBookCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getProductId(),
            JSON.toJSONString(command));
        super.send(record).get();
        logger.info("sendToMatchingEngine time : {}",System.currentTimeMillis()-t1);
    }

    @SneakyThrows
    public void sendToAccountant(AccountCommand command) {
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }

        String topic = appProperties.getAccountCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getUserId(),
            JSON.toJSONString(command));
        super.send(record).get();
    }

    @SneakyThrows
    public Future<RecordMetadata> sendToAccountantAsync(AccountCommand command,Callback callback) {
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }

        String topic = appProperties.getAccountCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getUserId(),
                JSON.toJSONString(command));
      return  super.send(record, callback);
    }
}
