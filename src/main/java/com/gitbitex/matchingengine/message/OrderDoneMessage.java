package com.gitbitex.matchingengine.message;

import java.math.BigDecimal;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDoneMessage extends OrderBookMessage {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private String doneReason;
    private String userId;

    public OrderDoneMessage() {
        this.setType(MessageType.ORDER_DONE);
    }
}
