package com.gitbitex.matchingengine;

import com.oracle.tools.packager.mac.MacAppBundler;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class MatchingEngineSnapshot {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds;
    private List<Product> products;
    private Set<Account> accounts;
    private Set<Order> orders;
    private List<OrderBookSnapshot> orderBookSnapshots;
    private long     time;
    private long beginCommandOffset;
    private long endCommandOffset;
    private Map<String,Long> tradeIds;
    private Map<String,Long> sequences;

}
