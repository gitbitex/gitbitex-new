package com.gitbitex.module.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.module.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMatchLog extends OrderBookLog {
    private long sequence;
    private long tradeId;
    private String takerOrderId;
    private String makerOrderId;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal funds;

    public OrderMatchLog() {
        this.setType(OrderBookLogType.MATCH);
    }

}
