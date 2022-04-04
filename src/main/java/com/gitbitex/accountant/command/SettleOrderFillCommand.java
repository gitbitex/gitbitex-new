package com.gitbitex.accountant.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettleOrderFillCommand extends AccountCommand {
    private String fillId;

    public SettleOrderFillCommand() {
        this.setType(CommandType.SETTLE_ORDER_FILL);
    }
}
