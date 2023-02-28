package com.gitbitex.matchingengine.log;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenLog extends OrderLog {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private OrderSide side;
    private String userId;

    public OrderOpenLog() {
        this.setType(LogType.ORDER_OPEN);
    }

}
