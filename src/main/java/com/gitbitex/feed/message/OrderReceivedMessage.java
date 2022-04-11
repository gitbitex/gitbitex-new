package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderReceivedMessage {
    private String type;
    private String time;
    private String productId;
    private long sequence;
    private String orderId;
    private String size;
    private String price;
    private String funds;
    private String side;
    private String orderType;

    public OrderReceivedMessage() {
        this.setType("received");
    }
}
