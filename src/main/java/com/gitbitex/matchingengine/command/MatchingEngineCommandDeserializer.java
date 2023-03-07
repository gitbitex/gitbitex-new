package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class MatchingEngineCommandDeserializer implements Deserializer<MatchingEngineCommand> {
    @Override
    @SneakyThrows
    public MatchingEngineCommand deserialize(String topic, byte[] bytes) {
        String jsonString = new String(bytes);

        try {
            MatchingEngineCommand command = JSON.parseObject(jsonString, MatchingEngineCommand.class);
            if (command.getType() == null) {
                logger.warn("Unknown command: {}", jsonString);
                return command;
            }

            switch (command.getType()) {
                case PUT_PRODUCT:
                    return JSON.parseObject(jsonString, PutProductCommand.class);
                case DEPOSIT:
                    return JSON.parseObject(jsonString, DepositCommand.class);
                case PLACE_ORDER:
                    return JSON.parseObject(jsonString, PlaceOrderCommand.class);
                case CANCEL_ORDER:
                    return JSON.parseObject(jsonString, CancelOrderCommand.class);
                default:
                    logger.warn("Unhandled order message type: {}", command.getType());
                    return command;
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + jsonString, e);
        }
    }
}
