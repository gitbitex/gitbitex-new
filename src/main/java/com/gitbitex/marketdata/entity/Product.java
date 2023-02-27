package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
@CompoundIndex(name = "idx_base_quote", def = "{'baseCurrency': 1, 'quoteCurrency': 1}", unique = true)
public class Product {

    private Date createdAt;

    private Date updatedAt;

    @Id
    private String productId;

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
