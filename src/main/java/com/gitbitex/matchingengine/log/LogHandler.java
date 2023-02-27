package com.gitbitex.matchingengine.log;

public interface LogHandler {
    void on(OrderRejectedLog log);

    void on(OrderReceivedLog log);

    void on(OrderOpenLog log);

    void on(OrderMatchLog log);

    void on(OrderDoneLog log);

    void on(OrderFilledMessage log);

    void on(AccountMessage log);

    void on(OrderMessage message);

    void on(TradeMessage message);
}
