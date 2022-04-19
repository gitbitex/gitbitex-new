package com.gitbitex.matchingengine.log;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class OrderBookLog {
    private String productId;
    private long sequence;
    private OrderBookLogType type;
    private long offset;
    private long commandOffset;
    private boolean commandFinished;
    private Date time;
}
