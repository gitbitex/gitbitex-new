package com.gitbitex.matchingengine.command;

import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewOrderCommand extends OrderBookCommand {
    private Order order;

    public NewOrderCommand() {
        this.setType(CommandType.NEW_ORDER);
    }
}
