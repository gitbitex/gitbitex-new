package com.gitbitex.matchingengine.command;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PutProductCommand extends MatchingEngineCommand {
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    public PutProductCommand() {
        this.setType(CommandType.PUT_PRODUCT);
    }
}
