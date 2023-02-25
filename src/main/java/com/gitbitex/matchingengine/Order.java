package com.gitbitex.matchingengine;

import com.gitbitex.marketdata.enums.OrderSide;
import com.gitbitex.marketdata.enums.OrderType;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Order implements Serializable {
    private String userId;
    private String orderId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private boolean postOnly;
    private Date time;

    public Order(){}

    public Order(PlaceOrderCommand command){
        this.userId=command.getUserId();
        this.orderId=command.getOrderId();
        this.type=command.getOrderType();
        this.side=command.getOrderSide();
        this.size=command.getSize();
        this.price=command.getPrice();
        this.funds=command.getFunds();
        this.time=command.getTime();
    }

    public Order copy() {
        Order copy = new Order();
        BeanUtils.copyProperties(this, copy);
        return copy;
    }
}
