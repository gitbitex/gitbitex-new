package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;
import org.apache.kafka.common.serialization.Serializer;

public class CommandSerializer implements Serializer<Command> {
    @Override
    public byte[] serialize(String s, Command command) {
        byte[] jsonBytes = JSON.toJSONBytes(command);
        byte[] messageBytes = new byte[jsonBytes.length + 1];
        messageBytes[0] = command.getType().getByteValue();
        System.arraycopy(jsonBytes, 0, messageBytes, 1, jsonBytes.length);
        return messageBytes;
    }
}
