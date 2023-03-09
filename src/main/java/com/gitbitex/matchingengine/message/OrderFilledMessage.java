package com.gitbitex.matchingengine.message;

import java.math.BigDecimal;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderFilledMessage extends OrderLog {
    private String orderId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private long tradeId;
    private OrderSide side;
    private String userId;

    public OrderFilledMessage() {
        this.setType(LogType.ORDER_FILLED);
    }
}
