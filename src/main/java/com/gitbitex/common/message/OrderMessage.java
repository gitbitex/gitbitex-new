package com.gitbitex.common.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessage {
    private long sequence;
    private String productId;
    private long offset;
    private CommandType type;

    public enum CommandType {
        ORDER_PLACED,
        PLACE_ORDER,
        ORDER_REJECTED,
        CANCEL_ORDER,
        ORDER_FILLED,
        ORDER_RECEIVED,
        ORDER_OPEN,
        ORDER_MATCH,
        ORDER_DONE,

        ACCOUNT_CHANGE,
        ACCOUNT_WITHDRAWAL,
        ACCOUNT_DEPOSIT,
        ACCOUNT_HOLD,
        ACCOUNT_UNHOLD,
        ACCOUNT_INCR_HOLD,
        ACCOUNT_INCR_AVAILABLE,

        DEPOSIT,
        WITHDRAWAL,

    }
}
