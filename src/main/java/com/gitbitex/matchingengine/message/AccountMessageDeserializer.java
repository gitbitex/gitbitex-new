package com.gitbitex.matchingengine.message;

import com.alibaba.fastjson.JSON;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

@Slf4j
public class AccountMessageDeserializer implements Deserializer<AccountMessage> {
    @Override
    @SneakyThrows
    public AccountMessage deserialize(String topic, byte[] bytes) {
        return JSON.parseObject(new String(bytes), AccountMessage.class);
    }
}
