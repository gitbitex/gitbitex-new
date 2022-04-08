package com.gitbitex.module.account.command;

import com.gitbitex.module.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceOrderCommand extends AccountCommand {
    private Order order;

    public PlaceOrderCommand() {
        this.setType(CommandType.PLACE_ORDER);
    }
}
