package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class Account {
    private String userId;
    private String currency;
    private BigDecimal available;
    private BigDecimal hold;

    public Account copy(){
        Account copy=new Account();
        copy.setUserId(userId);
        copy.setCurrency(currency);
        copy.setAvailable(available);
        copy.setHold(hold);
        return copy;
    }
}
