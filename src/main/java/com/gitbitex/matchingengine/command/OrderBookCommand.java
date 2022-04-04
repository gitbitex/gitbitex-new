package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookCommand {
    private String productId;
    private CommandType type;
    private long offset;

    public enum CommandType {
        NEW_ORDER,
        CANCEL_ORDER,
    }
}
