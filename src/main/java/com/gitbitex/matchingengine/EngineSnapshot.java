package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EngineSnapshot {
    private long commandOffset;
    private long logSequence;
    private List<String> orderIds = new ArrayList<>();
    private AccountBookSnapshot accountBookSnapshot;
    private List<OrderBookSnapshot> orderBookSnapshots;
}
