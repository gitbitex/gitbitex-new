package com.gitbitex.matchingengine.command;

import com.gitbitex.common.message.OrderMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderCommand extends MatchingEngineCommand {
    private String orderId;
    private String productId;

    public CancelOrderCommand() {
        this.setType(CommandType.CANCEL_ORDER);
    }
}
