package com.gitbitex.matchingengine.marketmessage;

import com.gitbitex.matchingengine.BookOrder;
import com.gitbitex.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class Level3OrderBookSnapshot extends MarketMessage {
    private long sequence;
    private long tradeId;
    private List<Level3SnapshotLine> asks = new ArrayList<>();
    private List<Level3SnapshotLine> bids = new ArrayList<>();

    public Level3OrderBookSnapshot() {
    }

    public Level3OrderBookSnapshot(OrderBook orderBook) {
        this.setType("l3_snapshot");
        this.setProductId(orderBook.getProductId());
        this.sequence = orderBook.getSequence().get();
        this.tradeId = orderBook.getTradeId().get();
        this.asks = orderBook.getAsks().getOrders().stream().map(Level3SnapshotLine::new).collect(Collectors.toList());
        this.bids = orderBook.getBids().getOrders().stream().map(Level3SnapshotLine::new).collect(Collectors.toList());
    }

    public static class Level3SnapshotLine extends ArrayList<Object> {
        public Level3SnapshotLine() {
        }

        public Level3SnapshotLine(BookOrder order) {
            this.add(order.getOrderId());
            this.add(order.getPrice().stripTrailingZeros().toPlainString());
            this.add(order.getSize().stripTrailingZeros().toPlainString());
            this.add(order.getUserId());
        }
    }

}
