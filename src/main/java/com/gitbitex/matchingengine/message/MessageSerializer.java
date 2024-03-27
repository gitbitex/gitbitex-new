package com.gitbitex.matchingengine.message;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.common.serialization.Serializer;

public class MessageSerializer implements Serializer<Message> {
    @Override
    public byte[] serialize(String s, Message command) {
        byte[] jsonBytes = JSON.toJSONBytes(command);
        byte[] messageBytes = new byte[jsonBytes.length + 1];
        messageBytes[0] = command.getMessageType().getByteValue();
        System.arraycopy(jsonBytes, 0, messageBytes, 1, jsonBytes.length);
        return messageBytes;
    }
}
