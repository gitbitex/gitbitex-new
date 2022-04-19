package com.gitbitex.order.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCommand {
    private Typ type;
    private String orderId;
    private long offset;

    public enum Typ {
        SAVE_ORDER,
        FILL_ORDER,
        UPDATE_ORDER_STATUS,
    }
}
