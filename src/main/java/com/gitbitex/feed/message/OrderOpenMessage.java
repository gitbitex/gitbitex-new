package com.gitbitex.feed.message;

import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderBookLogType;
import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenMessage  {
    private String type;
    private String productId;
    private long sequence;
    private String time;
    private String orderId;
    private String remainingSize;
    private String price;
    private String side;

    public OrderOpenMessage() {
        this.setType("open");
    }

}
