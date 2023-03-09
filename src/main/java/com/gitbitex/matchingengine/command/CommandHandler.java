package com.gitbitex.matchingengine.command;

public interface CommandHandler {

    void on(PutProductCommand command);

    void on(DepositCommand command);

    void on(PlaceOrderCommand command);

    void on(CancelOrderCommand command);
}
