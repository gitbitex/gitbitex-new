package com.gitbitex.matchingengine.command;

import java.math.BigDecimal;

import com.gitbitex.common.message.OrderMessage;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.signature.qual.ClassGetName;

@Getter
@Setter
public class DepositCommand extends MatchingEngineCommand {
    private String userId;
    private String currency;
    private BigDecimal amount;
    private String transactionId;
    public DepositCommand(){
        this.setType(CommandType.DEPOSIT);
    }
}
