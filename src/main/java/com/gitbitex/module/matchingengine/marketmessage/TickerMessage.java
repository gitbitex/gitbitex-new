package com.gitbitex.module.matchingengine.marketmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerMessage extends MarketMessage {
    private long tradeId;
    private long sequence;
    private String time;
    private String price;
    private String side;
    private String lastSize;
    private String open24h;
    private String close24h;
    private String high24h;
    private String low24h;
    private String volume24h;
    private String volume30d;

    public TickerMessage() {
        this.setType("ticker");
    }
}
