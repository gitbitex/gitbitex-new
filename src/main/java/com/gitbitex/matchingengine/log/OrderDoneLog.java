package com.gitbitex.matchingengine.log;

import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDoneLog extends OrderBookLog {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private DoneReason doneReason;
    private String userId;

    public OrderDoneLog() {
        this.setType(OrderBookLogType.DONE);
    }

    public enum DoneReason {
        FILLED,
        CANCELLED,
    }

}
