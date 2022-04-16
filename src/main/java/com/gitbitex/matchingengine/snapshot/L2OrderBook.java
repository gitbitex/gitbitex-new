package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.PageLine;
import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        this.asks = orderBook.getAsks().getLineByPrice().stream().map(Line::new).collect(Collectors.toList());
        this.bids = orderBook.getBids().getLineByPrice().stream().map(Line::new).collect(Collectors.toList());
    }

    public L2OrderBook(OrderBook orderBook, int maxSize) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence().get();
        this.asks = orderBook.getAsks().getLineByPrice().stream()
                .limit(maxSize)
                .map(Line::new)
                .collect(Collectors.toList());
        this.bids = orderBook.getBids().getLineByPrice().stream()
                .limit(maxSize)
                .map(Line::new)
                .collect(Collectors.toList());
    }

    public L2OrderBook truncate(int maxSize) {
        L2OrderBook orderBook = new L2OrderBook();
        orderBook.productId = this.getProductId();
        orderBook.sequence = this.getSequence();
        orderBook.asks = this.asks.stream().limit(maxSize).collect(Collectors.toList());
        orderBook.bids = this.bids.stream().limit(maxSize).collect(Collectors.toList());
        return orderBook;
    }

    @Nullable
    public List<L2OrderBookChange> diff(L2OrderBook newL2OrderBook) {
        if (newL2OrderBook.getSequence() < this.sequence) {
            throw new RuntimeException("new l2 order book is too old");
        }
        if (newL2OrderBook.getSequence() == this.sequence) {
            return null;
        }

        List<L2OrderBookChange> changes = new ArrayList<>();
        changes.addAll(diffLines(OrderSide.SELL, this.getAsks(), newL2OrderBook.getAsks()));
        changes.addAll(diffLines(OrderSide.BUY, this.getBids(), newL2OrderBook.getBids()));
        return changes;
    }

    private List<L2OrderBookChange> diffLines(OrderSide side, List<Line> oldLines, List<Line> newLines) {
        Map<String, Line> oldLineByPrice = new LinkedHashMap<>();
        Map<String, Line> newLineByPrice = new LinkedHashMap<>();
        for (Line oldLine : oldLines) {
            oldLineByPrice.put(oldLine.getPrice(), oldLine);
        }
        for (Line newLine : newLines) {
            newLineByPrice.put(newLine.getPrice(), newLine);
        }

        List<L2OrderBookChange> changes = new ArrayList<>();
        oldLineByPrice.forEach(((oldPrice, oldLine) -> {
            Line newLine = newLineByPrice.get(oldPrice);
            if (newLine == null) {
                L2OrderBookChange change = new L2OrderBookChange(side.name().toLowerCase(), oldPrice, "0");
                changes.add(change);
            } else if (!newLine.getSize().equals(oldLine.getSize())) {
                L2OrderBookChange change = new L2OrderBookChange(side.name().toLowerCase(), oldPrice,
                        newLine.getSize());
                changes.add(change);
            }
        }));
        newLineByPrice.forEach((newPrice, newLine) -> {
            Line oldLine = oldLineByPrice.get(newPrice);
            if (oldLine == null) {
                L2OrderBookChange change = new L2OrderBookChange(side.name().toLowerCase(), newPrice,
                        newLine.getSize());
                changes.add(change);
            }
        });
        return changes;
    }

    public static class Line extends ArrayList<Object> {
        public Line() {
        }

        public Line(PageLine line) {
            add(line.getPrice().stripTrailingZeros().toPlainString());
            add(line.getTotalSize().stripTrailingZeros().toPlainString());
            add(line.getOrderById().size());
        }

        public String getPrice() {
            return this.get(0).toString();
        }

        public String getSize() {
            return this.get(1).toString();
        }
    }
}
