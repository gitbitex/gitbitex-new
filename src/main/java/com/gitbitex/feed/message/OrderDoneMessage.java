package com.gitbitex.feed.message;

import lombok.Getter;
import lombok.Setter;

/**
 * {
 * "type": "done",
 * "time": "2014-11-07T08:19:27.028459Z",
 * "product_id": "BTC-USD",
 * "sequence": 10,
 * "price": "200.2",
 * "order_id": "d50ec984-77a8-460a-b958-66f114b0de9b",
 * "reason": "filled", // or "canceled"
 * "side": "sell",
 * "remaining_size": "0"
 * }
 */
@Getter
@Setter
public class OrderDoneMessage {
    private String type = "done";
    private String productId;
    private long sequence;
    private String orderId;
    private String remainingSize;
    private String price;
    private String side;
    private String reason;
    private String time;
}


