package com.gitbitex.module.matchingengine.log;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

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
