package com.gitbitex.matchingengine.log;

import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenLog extends OrderBookLog {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private Order.OrderSide side;
    private String userId;

    public OrderOpenLog() {
        this.setType(OrderBookLogType.OPEN);
    }

}
