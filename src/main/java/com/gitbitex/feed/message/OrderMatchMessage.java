package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMatchMessage {
    private String type = "match";
    private String productId;
    private long tradeId;
    private long sequence;
    private String takerOrderId;
    private String makerOrderId;
    private String time;
    private String size;
    private String price;
    private String side;
}
