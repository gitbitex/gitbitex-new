package com.gitbitex.matchingengine.message;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class OrderMessageDeserializer implements Deserializer<OrderMessage> {
    @Override
    public OrderMessage deserialize(String topic, byte[] bytes) {
        return JSON.parseObject(new String(bytes), OrderMessage.class);
    }
}
