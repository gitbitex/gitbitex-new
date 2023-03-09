package com.gitbitex.matchingengine.message;

import java.math.BigDecimal;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

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
