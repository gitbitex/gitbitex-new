package com.gitbitex.matchingengine.log;

import com.gitbitex.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderReceivedLog extends OrderBookLog {
    private Order order;

    public OrderReceivedLog() {
        this.setType(OrderBookLogType.RECEIVED);
    }
}
