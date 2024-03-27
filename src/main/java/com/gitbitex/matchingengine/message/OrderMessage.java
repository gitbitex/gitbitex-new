package com.gitbitex.matchingengine.message;

import com.gitbitex.matchingengine.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessage extends Message {
    private Order order;

    public OrderMessage() {
        this.setMessageType(MessageType.ORDER);
    }
}
