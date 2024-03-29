package com.gitbitex.wallet;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Transaction {
    private String id;
    private String userId;
    private long sequence;
    private String currency;
    private BigDecimal amount;
    private boolean submitted;
}
