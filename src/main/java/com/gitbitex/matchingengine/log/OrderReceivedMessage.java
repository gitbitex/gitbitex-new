package com.gitbitex.matchingengine.log;

import com.gitbitex.marketdata.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderReceivedMessage extends Log {
    private String productId;
    private Order order;

    public OrderReceivedMessage() {
        this.setType(LogType.ORDER_RECEIVED);
    }
}
