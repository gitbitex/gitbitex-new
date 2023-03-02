package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchingEngineSnapshot {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds;
    private List<Product> products;
    private List<Account> accounts;
    private List<OrderBookSnapshot> orderBookSnapshots;
    private long     time;
}
