package com.gitbitex.matchingengine.log;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountChangeLog extends Log {
    private String userId;
    private String currency;
    private BigDecimal hold;
    private BigDecimal available;
    private BigDecimal holdIncrement;
    private BigDecimal availableIncrement;
    private String transactionId;
    private ChangeReason reason;

    public AccountChangeLog() {
        this.setType(LogType.ACCOUNT_CHANGE);
    }

    public enum ChangeReason {
        DEPOSIT,
        WITHDRAWAL,
        HOLD,
        UNHOLD,
        EXCHANGE,
    }
}
