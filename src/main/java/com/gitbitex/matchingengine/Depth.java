package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

public class Depth extends TreeMap<BigDecimal, PriceGroupedOrderCollection> {

    public Depth(Comparator<BigDecimal> comparator) {
        super(comparator);
    }

    public void addOrder(Order order) {
        this.computeIfAbsent(order.getPrice(), k -> new PriceGroupedOrderCollection()).put(order.getId(), order);
    }

    public void removeOrder(Order order) {
        var orders = get(order.getPrice());
        if (orders == null) {
            return;
        }
        orders.remove(order.getId());
        if (orders.isEmpty()) {
            remove(order.getPrice());
        }
    }
}
