package com.gitbitex.matchingengine.command;

import lombok.Getter;

@Getter
public enum CommandType {
    PLACE_ORDER((byte) 1),
    CANCEL_ORDER((byte) 2),
    DEPOSIT((byte) 3),
    WITHDRAWAL((byte) 4),
    PUT_PRODUCT((byte) 5);

    private final byte byteValue;

    CommandType(byte value) {
        this.byteValue = value;
    }

    public static CommandType valueOfByte(byte b) {
        for (CommandType type : CommandType.values()) {
            if (b == type.byteValue) {
                return type;
            }
        }
        throw new RuntimeException("Unknown byte: " + b);
    }

}
