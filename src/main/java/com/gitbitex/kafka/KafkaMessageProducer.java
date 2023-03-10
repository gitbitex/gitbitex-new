package com.gitbitex.kafka;

import java.util.Properties;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.Account;
import com.gitbitex.matchingengine.Order;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.command.Command;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
public class KafkaMessageProducer extends KafkaProducer<String, String> {
    private final AppProperties appProperties;

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties) {
        super(kafkaProperties);
        this.appProperties = appProperties;
    }

    // 35038 3699
    // 41599 2570
    public void sendToMatchingEngine( Command command, Callback callback) {
        byte[] jsonBytes = JSON.toJSONBytes(command);
        byte[] messageBytes = new byte[jsonBytes.length + 1];
        messageBytes[0] = command.getType().getByteValue();
        System.arraycopy(jsonBytes, 0, messageBytes, 1, jsonBytes.length);
        ProducerRecord<String, String> record = new ProducerRecord<>(appProperties.getMatchingEngineCommandTopic(), new String(messageBytes));
        super.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public void sendAccount(Account account, Callback callback) {
        send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(),
            JSON.toJSONString(account)), (m, e) -> {
            if (e != null) {
                throw new RuntimeException(e);
            }
            callback.onCompletion(m, null);
        });
    }

    public void sendTrade(Trade trade, Callback callback) {
        send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(),
            JSON.toJSONString(trade)), (m, e) -> {
            if (e != null) {
                throw new RuntimeException(e);
            }
            callback.onCompletion(m, null);
        });
    }

    public void sendOrder(Order order, Callback callback) {
        send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), order.getId(),
            JSON.toJSONString(order)), (m, e) -> {
            if (e != null) {
                throw new RuntimeException(e);
            }
            callback.onCompletion(m, null);
        });
    }

}
