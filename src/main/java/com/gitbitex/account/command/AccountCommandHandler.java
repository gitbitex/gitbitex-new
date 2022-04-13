package com.gitbitex.account.command;

public interface AccountCommandHandler {
    void on(PlaceOrderCommand command);

    void on(CancelOrderCommand command);

    void on(SettleOrderFillCommand command);

    void on(SettleOrderCommand command);
}
