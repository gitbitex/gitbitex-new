package com.gitbitex.account.command;

import com.gitbitex.matchingengine.command.OrderBookCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderCommand extends AccountCommand {
    private String orderId;
    private String productId;

    public CancelOrderCommand() {
        this.setType(CommandType.CANCEL_ORDER);
    }
}
