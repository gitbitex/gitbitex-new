package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Bill {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String userId;
    private String currency;
    private BigDecimal holdIncrement;
    private BigDecimal availableIncrement;
    private String type;
    private boolean settled;
    private String notes;
}

