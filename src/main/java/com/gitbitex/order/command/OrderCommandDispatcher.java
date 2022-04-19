package com.gitbitex.order.command;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderCommandDispatcher {
    private final OrderCommandHandler handler;

    public void dispatch(OrderCommand command) {
        if (command instanceof SaveOrderCommand) {
            handler.on((SaveOrderCommand) command);
        } else if (command instanceof UpdateOrderStatusCommand) {
            handler.on((UpdateOrderStatusCommand) command);
        } else if (command instanceof FillOrderCommand) {
            handler.on((FillOrderCommand) command);
        } else {
            throw new RuntimeException("unknown order message: " + command.getClass().getName());
        }
    }
}
