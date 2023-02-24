package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;

import com.gitbitex.common.message.OrderMessage;
import com.gitbitex.common.message.OrderPlacedMessage;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class OrderMessageDeserializer implements Deserializer<Log> {
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
                    return JSON.parseObject(jsonString, AccountChangeMessage.class);
                case ORDER_DONE:
                    return JSON.parseObject(jsonString, OrderDoneMessage.class);
                case ORDER_MATCH:
                    return JSON.parseObject(jsonString, OrderMatchLog.class);
                case ORDER_OPEN:
                    return JSON.parseObject(jsonString, OrderOpenMessage.class);
                case ORDER_RECEIVED:
                    return JSON.parseObject(jsonString, OrderReceivedMessage.class);
                case ORDER_FILLED:
                    return JSON.parseObject(jsonString, OrderFilledMessage.class);
                case ORDER_REJECTED:
                    return JSON.parseObject(jsonString, OrderRejectedMessage.class);
                default:
                    logger.warn("Unhandled order message type: {}", orderMessage.getType());
                    return orderMessage;
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
