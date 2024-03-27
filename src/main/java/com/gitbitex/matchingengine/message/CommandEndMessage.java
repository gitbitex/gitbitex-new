package com.gitbitex.matchingengine.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommandEndMessage extends Message {
    private long commandOffset;

    public CommandEndMessage() {
        this.setMessageType(MessageType.COMMAND_END);
    }
}
