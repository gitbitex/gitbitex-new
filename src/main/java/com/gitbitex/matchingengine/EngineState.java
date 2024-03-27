package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class EngineState {
    private String id = "default";
    private Long commandOffset;
    private Long messageOffset;
    private Long messageSequence;
    private Map<String, Long> tradeSequences = new HashMap<>();
    private Map<String, Long> orderSequences = new HashMap<>();
}
