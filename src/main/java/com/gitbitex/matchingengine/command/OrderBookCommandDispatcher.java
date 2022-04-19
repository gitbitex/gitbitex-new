package com.gitbitex.matchingengine.command;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderBookCommandDispatcher {
    private final OrderBookCommandHandler handler;

    public void dispatch(OrderBookCommand orderBookCommand) {
        if (orderBookCommand instanceof NewOrderCommand) {
            handler.on((NewOrderCommand) orderBookCommand);
        } else if (orderBookCommand instanceof CancelOrderCommand) {
            handler.on((CancelOrderCommand) orderBookCommand);
        } else {
            throw new RuntimeException("unknown order message: " + orderBookCommand.getClass().getName());
        }
    }
}
