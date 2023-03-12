package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Account implements Cloneable {
    private String id;
    private String userId;
    private String currency;
    private BigDecimal available;
    private BigDecimal hold;

    @Override
    public Account clone() {
        try {
            return (Account) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
