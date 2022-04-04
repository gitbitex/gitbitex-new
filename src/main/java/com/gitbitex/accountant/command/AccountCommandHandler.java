package com.gitbitex.accountant.command;

public interface AccountCommandHandler {
    void on(PlaceOrderCommand command);

    void on(SettleOrderFillCommand command);

    void on(SettleOrderCommand command);
}
