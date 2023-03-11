package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import lombok.Getter;
import lombok.Setter;

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
        if (command.getUserId() == null) {
            throw new NullPointerException("userId");
        }
        if (command.getOrderId() == null) {
            throw new NullPointerException("orderId");
        }
        if (command.getOrderType() == null) {
            throw new NullPointerException("orderType");
        }
        if (command.getPrice() == null) {
            throw new NullPointerException("price");
        }

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
            return (Order)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
