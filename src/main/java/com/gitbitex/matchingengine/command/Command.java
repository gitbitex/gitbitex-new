package com.gitbitex.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Command {
    private long offset;
    private CommandType type;
}
