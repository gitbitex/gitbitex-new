package com.gitbitex.matchingengine.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.PageLine;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2OrderBook {
    private String productId;
    private long sequence;
    private List<Line> asks = new ArrayList<>();
    private List<Line> bids = new ArrayList<>();

    public L2OrderBook() {
    }

    public L2OrderBook(OrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        this.asks = orderBook.getAsks().getLines().stream().map(Line::new).collect(Collectors.toList());
        this.bids = orderBook.getBids().getLines().stream().map(Line::new).collect(Collectors.toList());
    }

    public L2OrderBook makeL1OrderBookSnapshot() {
        L2OrderBook snapshot = new L2OrderBook();
        snapshot.productId = this.getProductId();
        snapshot.sequence = this.sequence;
        if (!this.asks.isEmpty()) {
            snapshot.asks = Collections.singletonList(this.asks.get(0));
        }
        if (!this.bids.isEmpty()) {
            snapshot.bids = Collections.singletonList(this.bids.get(0));
        }
        return snapshot;
    }

    public static class Line extends ArrayList<Object> {
        public Line() {
        }

        public Line(PageLine line) {
            add(line.getPrice().stripTrailingZeros().toPlainString());
            add(line.getTotalSize().stripTrailingZeros().toPlainString());
            add(line.getOrders().size());
        }
    }
}
