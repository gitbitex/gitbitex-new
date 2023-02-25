package com.gitbitex.matchingengine.log;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Log {
    private long sequence;
    private long offset;
    private Date time;
    private LogType type;

    public enum LogType {
        ACCOUNT_CHANGE,
        ORDER_REJECTED,
        ORDER_RECEIVED,
        ORDER_OPEN,
        ORDER_MATCH,
        ORDER_FILLED,
        ORDER_DONE,
    }
}
