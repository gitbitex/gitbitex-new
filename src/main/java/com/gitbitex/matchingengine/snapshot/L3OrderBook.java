package com.gitbitex.matchingengine.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.Order;
import com.gitbitex.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L3OrderBook {
    private String productId;
    private long sequence;
    private long tradeId;
    private long time;
    private List<Line> asks;
    private List<Line> bids;

    public L3OrderBook() {
    }

    public L3OrderBook(OrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getLogSequence().get();
        this.tradeId = orderBook.getTradeId().get();
        this.time = System.currentTimeMillis();
        this.asks = orderBook.getAsks().getOrders().stream()
            .map(Line::new)
            .collect(Collectors.toList());
        this.bids = orderBook.getBids().getOrders().stream()
            .map(Line::new)
            .collect(Collectors.toList());
    }

    public static class Line extends ArrayList<Object> {
        public Line() {
        }

        public Line(Order order) {
            this.add(order.getOrderId());
            this.add(order.getPrice().stripTrailingZeros().toPlainString());
            this.add(order.getSize().stripTrailingZeros().toPlainString());
        }
    }
}
