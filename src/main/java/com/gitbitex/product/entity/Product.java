package com.gitbitex.product.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

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
