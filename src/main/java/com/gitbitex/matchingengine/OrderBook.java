package com.gitbitex.matchingengine;


import com.gitbitex.marketdata.enums.OrderSide;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class OrderBook {
    private final String productId;
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final BookPage asks;
    private final BookPage bids;

    public OrderBook(String productId, LogWriter logWriter,
                     AccountBook accountBook, ProductBook productBook,
                     AtomicLong sequence) {
        this.productId = productId;
        this.tradeId = new AtomicLong();
        this.sequence = sequence;
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, sequence, accountBook, productBook, logWriter);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, sequence, accountBook, productBook, logWriter);
    }

    public OrderBook(String productId, OrderBookSnapshot snapshot, LogWriter logWriter,
                     AccountBook accountBook, ProductBook productBook,
                     AtomicLong logSequence) {
        this.sequence = logSequence;
        this.productId = productId;
        if (snapshot != null) {
            this.tradeId = new AtomicLong(snapshot.getTradeId());
        } else {
            this.tradeId = new AtomicLong();
        }
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, sequence, accountBook, productBook, logWriter);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, sequence, accountBook, productBook, logWriter);
        if (snapshot != null) {
            if (snapshot.getAsks() != null) {
                snapshot.getAsks().forEach(this.asks::addOrder);
            }
            if (snapshot.getBids() != null) {
                snapshot.getBids().forEach(this.bids::addOrder);
            }
        }
    }

    public OrderBookSnapshot takeSnapshot() {
        List<Order> askOrders = this.asks.getOrders().stream()
                .map(Order::copy)
                .collect(Collectors.toList());
        List<Order> bidOrders = this.bids.getOrders().stream()
                .map(Order::copy)
                .collect(Collectors.toList());
        OrderBookSnapshot orderBookSnapshot = new OrderBookSnapshot();
        orderBookSnapshot.setProductId(productId);
        orderBookSnapshot.setTradeId(tradeId.get());
        orderBookSnapshot.setAsks(askOrders);
        orderBookSnapshot.setBids(bidOrders);
        return orderBookSnapshot;
    }

    public void placeOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            asks.executeCommand(order, bids);
        } else {
            bids.executeCommand(order, asks);
        }
    }

    public void cancelOrder(String orderId) {
        this.asks.cancelOrder(orderId);
        this.bids.cancelOrder(orderId);
    }
}
