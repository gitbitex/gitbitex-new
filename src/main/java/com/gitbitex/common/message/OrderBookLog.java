package com.gitbitex.common.message;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookLog extends OrderMessage {
    private long sequence;
    private long offset;
    private long commandOffset;
    @Deprecated
    private boolean commandFinished;
    private Date time;
}
