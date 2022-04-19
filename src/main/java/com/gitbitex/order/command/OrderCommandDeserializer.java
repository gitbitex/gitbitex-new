package com.gitbitex.order.command;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.common.serialization.Deserializer;

public class OrderCommandDeserializer implements Deserializer<OrderCommand> {
    @Override
    public OrderCommand deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            OrderCommand message = JSON.parseObject(jsonString, OrderCommand.class);
            switch (message.getType()) {
                case SAVE_ORDER:
                    return JSON.parseObject(jsonString, SaveOrderCommand.class);
                case UPDATE_ORDER_STATUS:
                    return JSON.parseObject(jsonString, UpdateOrderStatusCommand.class);
                case FILL_ORDER:
                    return JSON.parseObject(jsonString, FillOrderCommand.class);
                default:
                    throw new RuntimeException("unknown order message type: " + message.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
