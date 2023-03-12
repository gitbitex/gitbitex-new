package com.gitbitex.matchingengine.command;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class PlaceOrderCommand extends Command {
    private String productId;
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private OrderType orderType;
    private OrderSide orderSide;
    private Date time;

    public PlaceOrderCommand() {
        this.setType(CommandType.PLACE_ORDER);
    }
}
