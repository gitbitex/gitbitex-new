package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.matchingengine.BookOrder;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.SlidingBloomFilterSnapshot;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@Setter
public class OrderBookSnapshot {
    private String productId;
    private long sequence;
    private long tradeId;
    private long commandOffset;
    private long logOffset;
    private SlidingBloomFilterSnapshot orderIdFilterSnapshot;
    private List<BookOrder> asks;
    private List<BookOrder> bids;

    public OrderBookSnapshot() {
    }

    public OrderBookSnapshot(OrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        this.tradeId = orderBook.getTradeId().get();
        this.commandOffset = orderBook.getCommandOffset();
        this.logOffset = orderBook.getLogOffset();
        this.orderIdFilterSnapshot = new SlidingBloomFilterSnapshot(orderBook.getOrderIdFilter());
        this.asks = orderBook.getAsks().getOrders().stream().map(BookOrder::copy).collect(Collectors.toList());
        this.bids = orderBook.getBids().getOrders().stream().map(BookOrder::copy).collect(Collectors.toList());
    }

    public OrderBook restore() {
        return new OrderBook(productId, new AtomicLong(tradeId), new AtomicLong(sequence), commandOffset, logOffset, asks, bids, orderIdFilterSnapshot.restore());
    }
}
