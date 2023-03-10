package com.gitbitex.matchingengine.message;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

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
