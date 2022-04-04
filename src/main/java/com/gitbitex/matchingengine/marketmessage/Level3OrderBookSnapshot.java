package com.gitbitex.matchingengine.marketmessage;

import com.gitbitex.matchingengine.BookOrder;
import com.gitbitex.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Level3OrderBookSnapshot extends MarketMessage {
    private long sequence;
    private long lastTradeId;
    private long orderBookCommandOffset;
    private long orderBookLogOffset;
    private List<Level3SnapshotLine> asks = new ArrayList<>();
    private List<Level3SnapshotLine> bids = new ArrayList<>();

    public Level3OrderBookSnapshot() {
    }

    public Level3OrderBookSnapshot(OrderBook orderBook) {
        this.setType("l3_snapshot");
        this.setProductId(orderBook.getProductId());
        this.sequence = orderBook.getSequence().get();
        this.lastTradeId = orderBook.getTradeId().get();
        this.orderBookCommandOffset = orderBook.getOrderBookCommandOffset();
        this.orderBookLogOffset = orderBook.getOrderBookLogOffset();
        orderBook.getAsks().getOrders().forEach(x -> asks.add(new Level3SnapshotLine(x)));
        orderBook.getBids().getOrders().forEach(x -> bids.add(new Level3SnapshotLine(x)));
    }

    public static class Level3SnapshotLine extends ArrayList<Object> {
        public Level3SnapshotLine() {
            super();
            // 反序列化
        }

        public Level3SnapshotLine(BookOrder order) {
            this.add(order.getOrderId());
            this.add(order.getPrice().stripTrailingZeros().toPlainString());
            this.add(order.getSize().stripTrailingZeros().toPlainString());
            this.add(order.getUserId());
        }

        public BookOrder getOrder() {
            BookOrder order = new BookOrder();
            order.setOrderId(this.get(0).toString());
            order.setPrice(new BigDecimal(this.get(1).toString()));
            order.setSize(new BigDecimal(this.get(2).toString()));
            order.setUserId(this.get(3).toString());
            return order;
        }
    }

}
