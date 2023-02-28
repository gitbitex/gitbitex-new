package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountMessage extends Log {
    private String userId;
    private String currency;
    private BigDecimal hold;
    private BigDecimal available;

    public AccountMessage() {
        this.setType(LogType.ACCOUNT);
    }

    public enum ChangeReason {
        DEPOSIT,
        WITHDRAWAL,
        HOLD,
        UNHOLD,
        EXCHANGE,
    }
}
