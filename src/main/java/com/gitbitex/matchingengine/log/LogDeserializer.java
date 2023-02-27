package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class LogDeserializer implements Deserializer<Log> {
    @Override
    @SneakyThrows
    public Log deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            Log orderMessage = JSON.parseObject(jsonString, Log.class);
            if (orderMessage.getType() == null) {
                logger.warn("Unknown command: {}", jsonString);
                return orderMessage;
            }

            switch (orderMessage.getType()) {
                case ACCOUNT_CHANGE:
                    return JSON.parseObject(jsonString, AccountChangeLog.class);
                case ORDER_DONE:
                    return JSON.parseObject(jsonString, OrderDoneLog.class);
                case ORDER_MATCH:
                    return JSON.parseObject(jsonString, OrderMatchLog.class);
                case ORDER_OPEN:
                    return JSON.parseObject(jsonString, OrderOpenLog.class);
                case ORDER_RECEIVED:
                    return JSON.parseObject(jsonString, OrderReceivedLog.class);
                case ORDER_FILLED:
                    return JSON.parseObject(jsonString, OrderFilledMessage.class);
                case ORDER_REJECTED:
                    return JSON.parseObject(jsonString, OrderRejectedLog.class);
                default:
                    logger.warn("Unhandled order message type: {}", orderMessage.getType());
                    return orderMessage;
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
