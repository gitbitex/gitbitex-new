package com.gitbitex.module.matchingengine.marketmessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.gitbitex.module.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Level2OrderBookSnapshot extends MarketMessage {
    private long sequence;
    private List<Level2SnapshotLine> asks = new ArrayList<>();
    private List<Level2SnapshotLine> bids = new ArrayList<>();

    public Level2OrderBookSnapshot() {

    }

    public Level2OrderBookSnapshot(OrderBook orderBook, boolean onlyLevel1) {
        this.setType("snapshot");
        this.setProductId(orderBook.getProductId());
        this.sequence = orderBook.getSequence().get();
        if (onlyLevel1) {
            orderBook.getAsks().getLines().stream().findFirst().ifPresent(line -> {
                this.asks.add(new Level2SnapshotLine(line.getPrice(), line.getTotalSize(), line.getOrders().size()));
            });
            orderBook.getBids().getLines().stream().findFirst().ifPresent(line -> {
                this.bids.add(new Level2SnapshotLine(line.getPrice(), line.getTotalSize(), line.getOrders().size()));
            });
        } else {
            orderBook.getAsks().getLines()
                .forEach(
                    x -> this.asks.add(new Level2SnapshotLine(x.getPrice(), x.getTotalSize(), x.getOrders().size())));
            orderBook.getBids().getLines()
                .forEach(
                    x -> this.bids.add(new Level2SnapshotLine(x.getPrice(), x.getTotalSize(), x.getOrders().size())));
        }
    }

    public static class Level2SnapshotLine extends ArrayList<Object> {
        public Level2SnapshotLine() {
        }

        public Level2SnapshotLine(BigDecimal price, BigDecimal size, long count) {
            add(price.stripTrailingZeros().toPlainString());
            add(size.stripTrailingZeros().toPlainString());
            add(count);
        }
    }
}
