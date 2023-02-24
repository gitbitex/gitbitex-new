package com.gitbitex.matchingengine.log;

public interface LogHandler {
    void on(OrderRejectedMessage log);

    void on(OrderReceivedMessage log);

    void on(OrderOpenMessage log);

    void on(OrderMatchLog log);

    void on(OrderDoneMessage log);

    void on(OrderFilledMessage log);

    void on(AccountChangeMessage log);
}
