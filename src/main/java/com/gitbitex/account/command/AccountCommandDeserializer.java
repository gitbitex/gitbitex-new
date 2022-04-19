package com.gitbitex.account.command;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.Deserializer;

public class AccountCommandDeserializer implements Deserializer<AccountCommand> {
    @Override
    @SneakyThrows
    public AccountCommand deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            AccountCommand message = JSON.parseObject(jsonString, AccountCommand.class);
            switch (message.getType()) {
                case PLACE_ORDER:
                    return JSON.parseObject(jsonString, PlaceOrderCommand.class);
                case CANCEL_ORDER:
                    return JSON.parseObject(jsonString, CancelOrderCommand.class);
                case SETTLE_ORDER:
                    return JSON.parseObject(jsonString, SettleOrderCommand.class);
                case SETTLE_ORDER_FILL:
                    return JSON.parseObject(jsonString, SettleOrderFillCommand.class);
                default:
                    throw new RuntimeException("unknown order message type: " + message.getType());
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
