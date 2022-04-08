package com.gitbitex.module.order.command;

import com.gitbitex.module.order.entity.Order;
import com.gitbitex.module.matchingengine.log.OrderDoneLog;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusCommand extends OrderCommand {
    private String orderId;
    private Order.OrderStatus orderStatus;
    private OrderDoneLog.DoneReason doneReason;

    public UpdateOrderStatusCommand() {
        this.setType(Typ.UPDATE_ORDER_STATUS);
    }

}
