package com.gitbitex.matchingengine;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account {
    private String userId;
    private String currency;
    private BigDecimal available = new BigDecimal(0);
    private BigDecimal hold = new BigDecimal(0);
}
