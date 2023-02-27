package com.gitbitex.matchingengine.log;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
