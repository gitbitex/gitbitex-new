package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositCommand extends Command {
    private String userId;
    private String currency;
    private BigDecimal amount;
    private String transactionId;

    public DepositCommand() {
        this.setType(CommandType.DEPOSIT);
    }
}
