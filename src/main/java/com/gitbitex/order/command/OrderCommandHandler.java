package com.gitbitex.order.command;

public interface OrderCommandHandler {
    void on(SaveOrderCommand command);

    void on(UpdateOrderStatusCommand command);

    void on(FillOrderCommand command);
}
