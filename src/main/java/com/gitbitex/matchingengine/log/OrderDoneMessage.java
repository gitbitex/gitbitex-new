package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.marketdata.entity.Order.OrderSide;
import com.gitbitex.marketdata.entity.Order.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDoneMessage extends Log {
    private String productId;
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private DoneReason doneReason;
    private String userId;

    public OrderDoneMessage() {
        this.setType(LogType.ORDER_DONE);
    }

    public enum DoneReason {
        FILLED,
        CANCELLED,
    }

}
