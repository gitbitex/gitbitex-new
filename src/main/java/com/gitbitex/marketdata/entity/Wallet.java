package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Кошелёк пользователя для конкретной криптовалюты
 */
@Getter
@Setter
public class Wallet {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String userId;
    private String currency;
    private String address;
    private BigDecimal balance;
    private BigDecimal locked;
    private String status; // ACTIVE, FROZEN
}
