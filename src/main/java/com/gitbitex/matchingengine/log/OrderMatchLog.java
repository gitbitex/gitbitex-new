package com.gitbitex.matchingengine.log;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
