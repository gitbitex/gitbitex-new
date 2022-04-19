package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.Deserializer;

public class OrderBookLogDeserializer implements Deserializer<OrderBookLog> {
    @Override
    @SneakyThrows
    public OrderBookLog deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            OrderBookLog message = JSON.parseObject(jsonString, OrderBookLog.class);

            switch (message.getType()) {
                case RECEIVED:
                    return JSON.parseObject(jsonString, OrderReceivedLog.class);
                case OPEN:
                    return JSON.parseObject(jsonString, OrderOpenLog.class);
                case MATCH:
                    return JSON.parseObject(jsonString, OrderMatchLog.class);
                case DONE:
                    return JSON.parseObject(jsonString, OrderDoneLog.class);
                default:
                    throw new RuntimeException("unknown order message type: " + message.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
