package com.gitbitex.module.order.command;

public interface OrderCommandHandler {
    void on(SaveOrderCommand command);

    void on(UpdateOrderStatusCommand command);

    void on(FillOrderCommand command);
}
