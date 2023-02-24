package com.gitbitex.matchingengine.log;

import com.gitbitex.marketdata.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRejectedMessage extends Log {
  private String productId;
    private Order order;
    private RejectReason rejectReason;

    public OrderRejectedMessage() {
        this.setType(LogType.ORDER_REJECTED);
    }

    public enum RejectReason{
        INSUFFICIENT_BALANCE
    }
}
