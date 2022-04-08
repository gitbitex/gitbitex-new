package com.gitbitex.module.matchingengine.command;

import com.gitbitex.entity.Order;
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
