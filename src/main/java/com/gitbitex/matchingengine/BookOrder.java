package com.gitbitex.matchingengine;

import com.gitbitex.marketdata.entity.Order.OrderSide;
import com.gitbitex.marketdata.entity.Order.OrderType;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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

    public BookOrder(){}

    public BookOrder(PlaceOrderCommand command){
        this.userId=command.getUserId();
        this.orderId=command.getOrderId();
        this.type=command.getOrderType();
        this.side=command.getOrderSide();
        this.size=command.getSize();
        this.price=command.getPrice();
        this.funds=command.getFunds();
        this.time=command.getTime();
    }

    public BookOrder copy() {
        BookOrder copy = new BookOrder();
        BeanUtils.copyProperties(this, copy);
        return copy;
    }
}
