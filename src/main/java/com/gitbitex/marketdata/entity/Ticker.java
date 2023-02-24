package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ticker {
    private String productId;
    private long tradeId;
    private long sequence;
    private long orderBookLogOffset;
    private Date time;
    private BigDecimal price;
    private OrderSide side;
    private BigDecimal lastSize;
    private Long time24h;
    private BigDecimal open24h;
    private BigDecimal close24h;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal volume24h;
    private Long time30d;
    private BigDecimal open30d;
    private BigDecimal close30d;
    private BigDecimal high30d;
    private BigDecimal low30d;
    private BigDecimal volume30d;
}
