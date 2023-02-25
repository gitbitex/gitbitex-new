package com.gitbitex.matchingengine.log;

import com.gitbitex.marketdata.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class OrderRejectedLog extends Log {
    private String productId;
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private Order.OrderSide side;
    private Order.OrderType orderType;
    private String clientOid;
    private Date time;
    private RejectReason rejectReason;

    public OrderRejectedLog() {
        this.setType(LogType.ORDER_REJECTED);
    }

    public enum RejectReason{
        INSUFFICIENT_FUNDS
    }
}
