package com.gitbitex.matchingengine.command;

public interface MatchingEngineCommandHandler {
    void on(DepositCommand command);

    void on(PlaceOrderCommand command);

    void on(CancelOrderCommand command);
}
