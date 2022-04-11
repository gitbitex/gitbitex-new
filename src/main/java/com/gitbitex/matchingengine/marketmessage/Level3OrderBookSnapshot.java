package com.gitbitex.matchingengine.marketmessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.BookOrder;
import com.gitbitex.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Level3OrderBookSnapshot {
    private String productId;
    private long sequence;
    private long tradeId;
    private List<SnapshotLine> asks = new ArrayList<>();
    private List<SnapshotLine> bids = new ArrayList<>();

    public Level3OrderBookSnapshot() {
    }

    public Level3OrderBookSnapshot(OrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        this.tradeId = orderBook.getTradeId().get();
        this.asks = orderBook.getAsks().getOrders().stream().map(SnapshotLine::new).collect(Collectors.toList());
        this.bids = orderBook.getBids().getOrders().stream().map(SnapshotLine::new).collect(Collectors.toList());
    }

    public static class SnapshotLine extends ArrayList<Object> {
        public SnapshotLine() {
        }

        public SnapshotLine(BookOrder order) {
            this.add(order.getOrderId());
            this.add(order.getPrice().stripTrailingZeros().toPlainString());
            this.add(order.getSize().stripTrailingZeros().toPlainString());
        }
    }

}
