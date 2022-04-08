package com.gitbitex.module.order.command;

import com.gitbitex.module.order.entity.Order;
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
