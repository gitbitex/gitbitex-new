package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
    private Date time;

    public BookOrder copy(){
        BookOrder copy= new BookOrder();
        BeanUtils.copyProperties(this,copy);
        return copy;
    }
}
