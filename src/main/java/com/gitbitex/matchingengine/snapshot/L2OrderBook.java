package com.gitbitex.matchingengine.snapshot;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.matchingengine.Order;
import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.SimpleOrderBook;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
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
    private long time;
    private long commandOffset;
    private List<Line> asks = new ArrayList<>();
    private List<Line> bids = new ArrayList<>();

    public L2OrderBook(){}

    public L2OrderBook(String productId) {
        this.productId=productId;
    }

    public L2OrderBook(SimpleOrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getSequence();
        this.time = System.currentTimeMillis();
        this.asks = orderBook.getAsks().entrySet().stream()
            .limit(20)
            .map(x->new Line(x.getKey(),x.getValue().getRemainingSize(),x.getValue().size()))
            .collect(Collectors.toList());
        this.bids = orderBook.getBids().entrySet().stream()
            .limit(20)
            .map(x->new Line(x.getKey(),x.getValue().getRemainingSize(),x.getValue().size()))
            .collect(Collectors.toList());
        /*this.asks = orderBook.getAsks().getLines().stream()
            .map(Line::new)
            .collect(Collectors.toList());
        this.bids = orderBook.getBids().getLines().stream()
            .map(Line::new)
            .collect(Collectors.toList());*/
    }

    public void addOrder(Order order){

    }

    public L2OrderBook(OrderBook orderBook, int maxSize) {
        this.productId = orderBook.getProductId();
        this.sequence = orderBook.getLogSequence().get();
        this.time = System.currentTimeMillis();

        this.asks = orderBook.getAsks().entrySet().stream()
            .limit(maxSize)
            .map(x->new Line(x.getKey(),x.getValue().getRemainingSize(),x.getValue().size()))
            .collect(Collectors.toList());
        this.bids = orderBook.getBids().entrySet().stream()
            .limit(maxSize)
            .map(x->new Line(x.getKey(),x.getValue().getRemainingSize(),x.getValue().size()))
            .collect(Collectors.toList());
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
        changes.addAll(diff(OrderSide.SELL, this.getAsks(), newL2OrderBook.getAsks()));
        changes.addAll(diff(OrderSide.BUY, this.getBids(), newL2OrderBook.getBids()));
        return changes;
    }

    private List<L2OrderBookChange> diff(OrderSide side, List<Line> oldLines, List<Line> newLines) {
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

        public Line(BigDecimal price, BigDecimal totalSize, int orderCount) {
            add(price);
            add(totalSize);
            add(orderCount);
        }

        public String getPrice() {
            return this.get(0).toString();
        }

        public String getSize() {
            return this.get(1).toString();
        }
    }
}
