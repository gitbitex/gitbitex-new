package com.gitbitex.matchingengine.message;

import com.gitbitex.matchingengine.Trade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeMessage extends Message {
    private Trade trade;

    public TradeMessage() {
        this.setMessageType(MessageType.TRADE);
    }
}
