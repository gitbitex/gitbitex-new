package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.common.message.OrderMessage;
import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderFilledMessage extends Log {
    private String orderId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private String productId;
    private long tradeId;
    private Order.OrderSide side;
    private String userId;

    public OrderFilledMessage() {
        this.setType(LogType.ORDER_FILLED);
    }
}
