package com.gitbitex.matchingengine.log;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenMessage extends OrderLog {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private OrderSide side;
    private String userId;

    public OrderOpenMessage() {
        this.setType(LogType.ORDER_OPEN);
    }

}
