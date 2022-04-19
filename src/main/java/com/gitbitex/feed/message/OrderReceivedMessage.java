package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderReceivedMessage {
    private String type = "received";
    private String time;
    private String productId;
    private long sequence;
    private String orderId;
    private String size;
    private String price;
    private String funds;
    private String side;
    private String orderType;
}
