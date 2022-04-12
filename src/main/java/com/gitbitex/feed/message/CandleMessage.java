package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandleMessage {
    private String type = "candle";
    private String productId;
    private long sequence;
    private int granularity;
    private long time;
    private String open;
    private String close;
    private String high;
    private String low;
    private String volume;
}
