package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderOpenLog extends Log {
    private String productId;
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private OrderSide side;
    private String userId;

    public OrderOpenLog() {
        this.setType(LogType.ORDER_OPEN);
    }

}
