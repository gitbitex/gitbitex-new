package com.gitbitex.matchingengine.command;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

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
