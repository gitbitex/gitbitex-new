package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchingEngineCommand {
    private long offset;
    private CommandType type;
}
