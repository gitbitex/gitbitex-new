package com.gitbitex.matchingengine.message;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.Charset;

@Slf4j
public class MatchingEngineMessageDeserializer implements Deserializer<Message> {
    @Override
    public Message deserialize(String topic, byte[] bytes) {
        try {
            MessageType messageType = MessageType.valueOfByte(bytes[0]);
            switch (messageType) {
                case COMMAND_START:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            CommandStartMessage.class);
                case COMMAND_END:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            CommandEndMessage.class);
                case ACCOUNT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            AccountMessage.class);
                case PRODUCT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            ProductMessage.class);
                case ORDER:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            OrderMessage.class);
                case TRADE:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            TradeMessage.class);
                default:
                    logger.warn("Unhandled order message type: {}", messageType);
                    return JSON.parseObject(bytes, 1, bytes.length - 1, Charset.defaultCharset(),
                            Message.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + new String(bytes), e);
        }
    }
}

