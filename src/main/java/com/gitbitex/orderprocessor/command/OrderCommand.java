package com.gitbitex.orderprocessor.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCommand {
    private Typ type;
    private String orderId;

    public enum Typ {
        SAVE_ORDER,
        FILL_ORDER,
        UPDATE_ORDER_STATUS,
    }
}
