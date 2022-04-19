package com.gitbitex.account.command;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccountCommandDispatcher {
    private final AccountCommandHandler handler;

    public void dispatch(AccountCommand command) {
        if (command instanceof PlaceOrderCommand) {
            handler.on((PlaceOrderCommand) command);
        } else if (command instanceof CancelOrderCommand) {
            handler.on((CancelOrderCommand) command);
        } else if (command instanceof SettleOrderCommand) {
            handler.on((SettleOrderCommand) command);
        } else if (command instanceof SettleOrderFillCommand) {
            handler.on((SettleOrderFillCommand) command);
        } else {
            throw new RuntimeException("unknown order message: " + command.getClass().getName());
        }
    }
}
