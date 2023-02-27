package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
public class Candle {
    @Id
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

    private long orderBookLogOffset;

    private long tradeId;
}
