package com.gitbitex.matchingengine.log;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class TradeMessageDeserializer implements Deserializer<TradeMessage> {
    @Override
    @SneakyThrows
    public TradeMessage deserialize(String topic, byte[] bytes) {
        return JSON.parseObject(new String(bytes), TradeMessage.class);
    }
}
