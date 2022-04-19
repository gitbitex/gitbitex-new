package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.Deserializer;

public class OrderBookCommandDeserializer implements Deserializer<OrderBookCommand> {
    @Override
    @SneakyThrows
    public OrderBookCommand deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            OrderBookCommand message = JSON.parseObject(jsonString, OrderBookCommand.class);
            switch (message.getType()) {
                case NEW_ORDER:
                    return JSON.parseObject(jsonString, NewOrderCommand.class);
                case CANCEL_ORDER:
                    return JSON.parseObject(jsonString, CancelOrderCommand.class);
                default:
                    throw new RuntimeException("unknown order message type: " + message.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
