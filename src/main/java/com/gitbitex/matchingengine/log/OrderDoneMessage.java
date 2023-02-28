package com.gitbitex.matchingengine.log;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDoneMessage extends OrderLog {
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
