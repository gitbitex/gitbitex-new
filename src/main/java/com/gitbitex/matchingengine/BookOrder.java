package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.math.BigDecimal;

import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class BookOrder implements Serializable {
    private String userId;
    private String orderId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private boolean postOnly;

    public BookOrder() {
    }

    public BookOrder(Order order) {
        BeanUtils.copyProperties(order, this);
    }
}
