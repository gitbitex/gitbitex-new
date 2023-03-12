package com.gitbitex.marketdata.entity;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Trade {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private long sequence;
    private String productId;
    private String takerOrderId;
    private String makerOrderId;
    private BigDecimal price;
    private BigDecimal size;
    private OrderSide side;
    private Date time;
}
