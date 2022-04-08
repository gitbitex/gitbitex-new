package com.gitbitex.matchingengine.log;

public interface OrderBookLogHandler {
    void on(OrderReceivedLog log);

    void on(OrderOpenLog log);

    void on(OrderMatchLog log);

    void on(OrderDoneLog log);
}
