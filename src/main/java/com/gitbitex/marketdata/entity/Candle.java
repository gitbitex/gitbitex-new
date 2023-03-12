package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Candle {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String productId;
    private int granularity;
    private long time;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private long tradeId;
}
