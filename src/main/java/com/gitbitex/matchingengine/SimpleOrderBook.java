package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;

import java.util.Comparator;

@Getter
public class SimpleOrderBook {
    private final String productId;
    private final Depth asks = new Depth(Comparator.naturalOrder());
    private final Depth bids = new Depth(Comparator.reverseOrder());
    private long sequence;

    public SimpleOrderBook(String productId) {
        this.productId = productId;
    }

    public SimpleOrderBook(String productId, long sequence) {
        this.productId = productId;
        this.sequence = sequence;
    }

    public void addOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.addOrder(order);
        sequence = order.getSequence();
    }

    public void removeOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.removeOrder(order);
        sequence =order.getSequence();
    }
}
