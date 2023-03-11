package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Product {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal baseMinSize;
    private BigDecimal baseMaxSize;
    private BigDecimal quoteMinSize;
    private BigDecimal quoteMaxSize;
    private int baseScale;
    private int quoteScale;
    private float quoteIncrement;
    private float takerFeeRate;
    private float makerFeeRate;
    private int displayOrder;
}
