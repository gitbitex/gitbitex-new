package com.gitbitex.account.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCommand {
    private String userId;
    private CommandType type;

    public enum CommandType {
        PLACE_ORDER,
        CANCEL_ORDER,
        SETTLE_ORDER,
        SETTLE_ORDER_FILL,
    }
}
