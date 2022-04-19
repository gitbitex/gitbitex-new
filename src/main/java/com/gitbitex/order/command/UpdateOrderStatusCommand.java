package com.gitbitex.order.command;

import com.gitbitex.matchingengine.log.OrderDoneLog.DoneReason;
import com.gitbitex.order.entity.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusCommand extends OrderCommand {
    private String orderId;
    private Order.OrderStatus orderStatus;
    private DoneReason doneReason;

    public UpdateOrderStatusCommand() {
        this.setType(Typ.UPDATE_ORDER_STATUS);
    }

}
