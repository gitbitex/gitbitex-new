package com.gitbitex.matchingengine;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class MatchingEngineSnapshot implements AutoCloseable {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds;
    private List<Product> products;
    private Set<Account> accounts;
    private Set<Order> orders;
    private List<OrderBookSnapshot> orderBookSnapshots;
    private long time;
    private long beginCommandOffset;
    private long endCommandOffset;
    private Map<String, Long> tradeIds;
    private Map<String, Long> sequences;

    public MatchingEngineSnapshot(){

    }


    @Override
    public void close() throws Exception {

    }
}
