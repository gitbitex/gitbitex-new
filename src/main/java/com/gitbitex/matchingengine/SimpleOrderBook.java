package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

@Getter
public class SimpleOrderBook {
    private final String productId;
    private final Depth asks = new Depth(Comparator.naturalOrder());
    private final Depth bids = new Depth(Comparator.reverseOrder());
    @Setter
    private long messageSequence;

    public SimpleOrderBook(String productId) {
        this.productId = productId;
    }

    public SimpleOrderBook(String productId, long messageSequence) {
        this.productId = productId;
        this.messageSequence = messageSequence;
    }

    public void addOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.addOrder(order);
    }

    public void removeOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.removeOrder(order);
    }
}
