package com.gitbitex.module.matchingengine.log;

import com.gitbitex.module.order.entity.Order;
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
