package com.gitbitex.matchingengine.message;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Log {
    private long sequence;
    private long offset;
    private Date time;
    private LogType type;

    public enum LogType {
        TICKER,
        ORDER,
        ACCOUNT,
        TRADE,
        BILL,
        ORDER_REJECTED,
        ORDER_RECEIVED,
        ORDER_OPEN,
        ORDER_MATCH,
        ORDER_FILLED,
        ORDER_DONE,
    }
}
