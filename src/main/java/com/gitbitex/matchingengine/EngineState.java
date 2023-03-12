package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EngineState {
    private String id = "default";
    private Long commandOffset;
}
