package com.gitbitex.matchingengine;


import com.gitbitex.enums.OrderSide;
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
    private final AtomicLong logSequence;
    private final BookPage asks;
    private final BookPage bids;

    public OrderBook(String productId, LogWriter logWriter,
                     AccountBook accountBook, ProductBook productBook,
                     AtomicLong logSequence) {
        this.productId = productId;
        this.tradeId = new AtomicLong();
        this.logSequence = logSequence;
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, logSequence, accountBook, productBook, logWriter, null);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, logSequence, accountBook, productBook, logWriter, null);
    }

    public OrderBook(OrderBookSnapshot snapshot, LogWriter logWriter,
                     AccountBook accountBook, ProductBook productBook,
                     AtomicLong logSequence) {
        this.logSequence = logSequence;
        this.productId = snapshot.getProductId();
        this.tradeId = new AtomicLong(snapshot.getTradeId());
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, this.logSequence, accountBook, productBook, logWriter, snapshot.getAsks());
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, this.logSequence, accountBook, productBook, logWriter, snapshot.getBids());
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
