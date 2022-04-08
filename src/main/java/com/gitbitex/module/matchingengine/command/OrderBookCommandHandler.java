package com.gitbitex.module.matchingengine.command;

public interface OrderBookCommandHandler {
    void on(NewOrderCommand command);

    void on(CancelOrderCommand command);
}
