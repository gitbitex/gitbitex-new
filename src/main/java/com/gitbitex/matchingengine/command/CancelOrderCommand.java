package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderCommand extends OrderBookCommand {
    private String userId;
    private String orderId;

    public CancelOrderCommand() {
        this.setType(CommandType.CANCEL_ORDER);
    }
}
