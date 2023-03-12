package com.gitbitex.matchingengine.message;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenMessage extends OrderBookMessage {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private OrderSide side;
    private String userId;

    public OrderOpenMessage() {
        this.setType(MessageType.ORDER_OPEN);
    }

}
