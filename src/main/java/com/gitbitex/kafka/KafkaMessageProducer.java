package com.gitbitex.kafka;

import java.util.Properties;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.account.command.AccountCommand;
import com.gitbitex.matchingengine.command.OrderBookCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
public class KafkaMessageProducer extends KafkaProducer<String, String> {
    private final AppProperties appProperties;

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties) {
        super(kafkaProperties);
        this.appProperties = appProperties;
    }

    @SneakyThrows
    public void sendToMatchingEngine(OrderBookCommand command) {
        if (command.getProductId() == null) {
            throw new NullPointerException("productId");
        }

        String topic = command.getProductId() + "-" + appProperties.getOrderBookCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getProductId(),
            JSON.toJSONString(command));
        super.send(record).get();
    }

    @SneakyThrows
    public void sendToAccountant(AccountCommand command) {
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }

        String topic = appProperties.getAccountCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, command.getUserId(),
            JSON.toJSONString(command));
        logger.info("{}",JSON.toJSONString(command));
        super.send(record).get();
    }
}
