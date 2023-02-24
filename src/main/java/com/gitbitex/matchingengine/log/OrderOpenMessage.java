package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.common.message.OrderBookLog;
import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderOpenMessage extends Log {
    private String productId;
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private Order.OrderSide side;
    private String userId;

    public OrderOpenMessage() {
        this.setType(LogType.ORDER_OPEN);
    }

}
