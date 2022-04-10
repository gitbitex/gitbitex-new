package com.gitbitex.matchingengine.marketmessage;

import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.PageLine;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class L2OrderBookSnapshot extends MarketMessage {
    private long sequence;
    private List<Level2SnapshotLine> asks = new ArrayList<>();
    private List<Level2SnapshotLine> bids = new ArrayList<>();

    public L2OrderBookSnapshot() {

    }

    public L2OrderBookSnapshot(OrderBook orderBook, boolean onlyLevel1) {
        this.setType("snapshot");
        this.setProductId(orderBook.getProductId());
        this.sequence = orderBook.getSequence().get();
        if (onlyLevel1) {
            orderBook.getAsks().getLines().stream().findFirst().ifPresent(x -> this.asks.add(new Level2SnapshotLine(x)));
            orderBook.getBids().getLines().stream().findFirst().ifPresent(x -> this.bids.add(new Level2SnapshotLine(x)));
        } else {
            this.asks = orderBook.getAsks().getLines().stream().map(Level2SnapshotLine::new).collect(Collectors.toList());
            this.bids = orderBook.getBids().getLines().stream().map(Level2SnapshotLine::new).collect(Collectors.toList());
        }
    }

    public L2OrderBookSnapshot(String productId, long sequence, Collection<PageLine> asks, Collection<PageLine> bids) {
        this.setProductId(productId);
        this.sequence = sequence;
        this.asks = asks.stream().map(Level2SnapshotLine::new).collect(Collectors.toList());
        this.bids = bids.stream().map(Level2SnapshotLine::new).collect(Collectors.toList());
    }

    public static class Level2SnapshotLine extends ArrayList<Object> {
        public Level2SnapshotLine() {
        }

        public Level2SnapshotLine(PageLine line) {
            add(line.getPrice().stripTrailingZeros().toPlainString());
            add(line.getTotalSize().stripTrailingZeros().toPlainString());
            add(line.getOrders().size());
        }
    }
}
