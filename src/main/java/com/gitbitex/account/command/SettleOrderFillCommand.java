package com.gitbitex.account.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettleOrderFillCommand extends AccountCommand {
    private String fillId;
    private String productId;

    public SettleOrderFillCommand() {
        this.setType(CommandType.SETTLE_ORDER_FILL);
    }
}
