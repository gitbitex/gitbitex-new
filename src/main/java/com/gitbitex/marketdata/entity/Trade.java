package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trade {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private long tradeId;
    private String productId;
    private String takerOrderId;
    private String makerOrderId;
    private BigDecimal price;
    private BigDecimal size;
    private OrderSide side;
    private Date time;
}
