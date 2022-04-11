package com.gitbitex.matchingengine.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.PageLine;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2OrderBookSnapshot {
    private String productId;
    private long sequence;
    private List<SnapshotLine> asks = new ArrayList<>();
    private List<SnapshotLine> bids = new ArrayList<>();

    public L2OrderBookSnapshot() {

    }

    public L2OrderBookSnapshot(OrderBook orderBook, boolean onlyLevel1) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        if (onlyLevel1) {
            orderBook.getAsks().getLines().stream().findFirst().ifPresent(x -> this.asks.add(new SnapshotLine(x)));
            orderBook.getBids().getLines().stream().findFirst().ifPresent(x -> this.bids.add(new SnapshotLine(x)));
        } else {
            this.asks = orderBook.getAsks().getLines().stream().map(SnapshotLine::new).collect(Collectors.toList());
            this.bids = orderBook.getBids().getLines().stream().map(SnapshotLine::new).collect(Collectors.toList());
        }
    }

    public L2OrderBookSnapshot(String productId, long sequence, Collection<PageLine> asks, Collection<PageLine> bids) {
        this.setProductId(productId);
        this.sequence = sequence;
        this.asks = asks.stream().map(SnapshotLine::new).collect(Collectors.toList());
        this.bids = bids.stream().map(SnapshotLine::new).collect(Collectors.toList());
    }

    public L2OrderBookSnapshot makeL1OrderBookSnapshot() {
        L2OrderBookSnapshot snapshot = new L2OrderBookSnapshot();
        snapshot.setProductId(this.getProductId());
        snapshot.sequence = this.sequence;
        if (!this.asks.isEmpty()) {
            snapshot.asks = Collections.singletonList(this.asks.get(0));
        }
        if (!this.bids.isEmpty()) {
            snapshot.bids = Collections.singletonList(this.bids.get(0));
        }
        return snapshot;
    }

    public static class SnapshotLine extends ArrayList<Object> {
        public SnapshotLine() {
        }

        public SnapshotLine(PageLine line) {
            add(line.getPrice().stripTrailingZeros().toPlainString());
            add(line.getTotalSize().stripTrailingZeros().toPlainString());
            add(line.getOrders().size());
        }
    }
}
