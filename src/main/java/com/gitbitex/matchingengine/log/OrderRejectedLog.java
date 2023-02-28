package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRejectedLog extends OrderLog {
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private OrderSide side;
    private OrderType orderType;
    private String clientOid;
    private Date time;
    private RejectReason rejectReason;

    public OrderRejectedLog() {
        this.setType(LogType.ORDER_REJECTED);
    }

    public enum RejectReason {
        INSUFFICIENT_FUNDS
    }
}
