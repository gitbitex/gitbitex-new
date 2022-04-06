package com.gitbitex.matchingengine.marketmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandleMessage extends MarketMessage {
    private long sequence;
    private int granularity;
    private String time;
    private String open;
    private String close;
    private String high;
    private String low;
    private String volume;

    public CandleMessage() {
        this.setType("candle");
    }
}
