package com.gitbitex.matchingengine.message;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class OrderBookMessage {
    private String productId;
    private long sequence;
    private Date time;
    private MessageType type;

    public enum MessageType {
        ORDER_RECEIVED,
        ORDER_OPEN,
        ORDER_MATCH,
        ORDER_DONE,
    }
}
