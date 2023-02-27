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
public class Bill {
    @Id
    private String id;

    private Date createdAt;

    private Date updatedAt;

    private String billId;

    private String userId;

    private String currency;

    private BigDecimal holdIncrement;

    private BigDecimal availableIncrement;

    private String type;

    private boolean settled;

    private String notes;
}

