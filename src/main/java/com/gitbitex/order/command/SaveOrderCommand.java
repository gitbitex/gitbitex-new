package com.gitbitex.order.command;

import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveOrderCommand extends OrderCommand {
    private Order order;

    public SaveOrderCommand() {
        this.setType(Typ.SAVE_ORDER);
    }

}
