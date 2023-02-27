package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessage extends Log {
    private String orderId;

    private String productId;

    private String userId;

    private String clientOid;

    private Date time;

    private BigDecimal size;

    private BigDecimal funds;

    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;

    private BigDecimal filledSize;

    private BigDecimal executedValue;

    private BigDecimal price;

    private BigDecimal fillFees;

    private OrderType orderType;

    private OrderSide side;

    private OrderStatus status;

    public OrderMessage(){
        this.setType(LogType.ORDER);
    }
}
