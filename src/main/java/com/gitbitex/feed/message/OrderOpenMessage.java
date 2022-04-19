package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderOpenMessage {
    private String type = "open";
    private String productId;
    private long sequence;
    private String time;
    private String orderId;
    private String remainingSize;
    private String price;
    private String side;
}
