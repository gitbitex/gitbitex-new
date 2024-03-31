package com.gitbitex.matchingengine.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Message {
    private long sequence;
    private MessageType messageType;
}
