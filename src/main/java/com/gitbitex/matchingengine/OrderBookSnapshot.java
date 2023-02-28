package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class OrderBookSnapshot {
    private String productId;
    private long tradeId;
    private long logSequence;
    private List<Order> asks;
    private List<Order> bids;

    public OrderBookSnapshot() {
    }

    public OrderBookSnapshot(OrderBook orderBook) {
        List<Order> askOrders = orderBook.getAsks().values().stream()
                .flatMap(x -> x.values().stream())
                .map(Order::copy)
                .collect(Collectors.toList());
        List<Order> bidOrders = orderBook.getBids().values().stream()
                .flatMap(x -> x.values().stream())
                .map(Order::copy)
                .collect(Collectors.toList());

        this.setProductId(productId);
        this.setTradeId(orderBook.getTradeId().get());
        this.setLogSequence(orderBook.getLogSequence().get());
        this.setAsks(askOrders);
        this.setBids(bidOrders);
    }
}
