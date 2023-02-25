package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public   class AccountChangeLog extends Log {
    private String userId;
    private String currency;
    private BigDecimal hold;
    private BigDecimal available;
    private BigDecimal holdIncr;
    private BigDecimal availableIncr;
    private String transactionId;
    private ChangeReason reason;

    public AccountChangeLog(){
        this.setType(LogType.ACCOUNT_CHANGE);
    }

    public enum ChangeReason{
        DEPOSIT,
        WITHDRAWAL,
        HOLD,
        UNHOLD,
        EXCHANGE,
    }
}
