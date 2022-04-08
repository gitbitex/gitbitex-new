package com.gitbitex.account.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettleOrderCommand extends AccountCommand {
    private String orderId;

    public SettleOrderCommand() {
        this.setType(CommandType.SETTLE_ORDER);
    }
}
