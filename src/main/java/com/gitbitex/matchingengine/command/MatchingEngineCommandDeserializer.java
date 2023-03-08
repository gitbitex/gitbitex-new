package com.gitbitex.matchingengine.command;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class MatchingEngineCommandDeserializer implements Deserializer<MatchingEngineCommand> {
    @Override
    @SneakyThrows
    public MatchingEngineCommand deserialize(String topic, byte[] bytes) {
        try {
            CommandType commandType = CommandType.valueOfByte(bytes[0]);
            switch (commandType) {
                case PUT_PRODUCT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                        PutProductCommand.class);
                case DEPOSIT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(), DepositCommand.class);
                case PLACE_ORDER:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                        PlaceOrderCommand.class);
                case CANCEL_ORDER:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                        CancelOrderCommand.class);
                default:
                    logger.warn("Unhandled order message type: {}", commandType);
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                        MatchingEngineCommand.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + new String(bytes), e);
        }
    }
}

