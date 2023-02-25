package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDoneLog extends Log {
    private String productId;
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private DoneReason doneReason;
    private String userId;

    public OrderDoneLog() {
        this.setType(LogType.ORDER_DONE);
    }

    public enum DoneReason {
        FILLED,
        CANCELLED,
    }

}
