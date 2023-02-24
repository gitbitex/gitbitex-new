package com.gitbitex.matchingengine.log;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Log {
    private long sequence;
    private long offset;
    private long commandOffset;
    @Deprecated
    private boolean commandFinished;
    private Date time;
    private LogType type;
    public enum LogType{
        ACCOUNT_CHANGE,
        ORDER_REJECTED,
        ORDER_RECEIVED,
        ORDER_OPEN,
        ORDER_MATCH,
        ORDER_FILLED,
        ORDER_DONE,
    }
}
