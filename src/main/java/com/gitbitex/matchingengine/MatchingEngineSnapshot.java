package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.List;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingEngineSnapshot {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds = new ArrayList<>();
    private AccountBookSnapshot accountBookSnapshot;
    private List<OrderBookSnapshot> orderBookSnapshots;
}
