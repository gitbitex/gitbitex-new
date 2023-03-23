package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Order implements Cloneable {
    private String id;
    private long sequence;
    private String userId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private BigDecimal remainingFunds;
    private BigDecimal size;
    private BigDecimal funds;
    private boolean postOnly;
    private Date time;
    private String productId;
    private OrderStatus status;
    private String clientOid;

    public Order() {
    }

    public Order(PlaceOrderCommand command) {
        this.productId = command.getProductId();
        this.userId = command.getUserId();
        this.id = command.getOrderId();
        this.type = command.getOrderType();
        this.side = command.getOrderSide();
        this.price = command.getPrice();
        this.size = command.getSize();
        if (command.getOrderType() == OrderType.LIMIT) {
            this.funds = command.getSize().multiply(command.getPrice());
        } else {
            this.funds = command.getFunds();
        }
        this.remainingSize = this.size;
        this.remainingFunds = this.funds;
        this.time = command.getTime();
    }

    @Override
    public Order clone() {
        try {
            return (Order) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
