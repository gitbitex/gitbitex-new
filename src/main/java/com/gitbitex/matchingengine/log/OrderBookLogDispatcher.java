package com.gitbitex.matchingengine.log;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderBookLogDispatcher {
    private final OrderBookLogHandler handler;

    public void dispatch(OrderBookLog log) {
        if (log instanceof OrderReceivedLog) {
            handler.on((OrderReceivedLog) log);
        } else if (log instanceof OrderOpenLog) {
            handler.on((OrderOpenLog) log);
        } else if (log instanceof OrderMatchLog) {
            handler.on((OrderMatchLog) log);
        } else if (log instanceof OrderDoneLog) {
            handler.on((OrderDoneLog) log);
        } else {
            throw new RuntimeException("unknown order message: " + log.getClass().getName());
        }
    }
}
