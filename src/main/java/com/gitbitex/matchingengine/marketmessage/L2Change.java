package com.gitbitex.matchingengine.marketmessage;

import java.math.BigDecimal;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2Change {
    private String productId;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal size;
}
