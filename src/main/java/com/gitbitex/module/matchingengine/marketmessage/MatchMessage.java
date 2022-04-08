package com.gitbitex.module.matchingengine.marketmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchMessage extends MarketMessage {
    private long tradeId;
    private long sequence;
    private String takerOrderId;
    private String makerOrderId;
    private String time;
    private String size;
    private String price;
    private String side;

    public MatchMessage() {
        this.setType("match");
    }
}
