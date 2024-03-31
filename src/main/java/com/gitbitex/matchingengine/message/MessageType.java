package com.gitbitex.matchingengine.message;

import lombok.Getter;

@Getter
public enum MessageType {
    ACCOUNT((byte) 1),
    PRODUCT((byte) 2),
    ORDER((byte) 3),
    TRADE((byte) 4),
    COMMAND_START((byte) 5),
    COMMAND_END((byte) 6);

    private final byte byteValue;

    MessageType(byte value) {
        this.byteValue = value;
    }

    public static MessageType valueOfByte(byte b) {
        for (MessageType type : MessageType.values()) {
            if (b == type.byteValue) {
                return type;
            }
        }
        throw new RuntimeException("Unknown byte: " + b);
    }

}
