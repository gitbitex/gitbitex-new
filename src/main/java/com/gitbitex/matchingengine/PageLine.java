package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;

import lombok.Getter;

public class PageLine implements Serializable {
    @Getter
    private final BigDecimal price;
    private final LinkedHashMap<String, Order> orderById = new LinkedHashMap<>();


    public PageLine(BigDecimal price) {
        this.price = price;

    }

    public void addOrder(Order order) {
        orderById.put(order.getOrderId(), order);
    }

    public void decreaseOrderSize(String orderId, BigDecimal size) {
        Order order = orderById.get(orderId);
        order.setSize(order.getSize().subtract(size));
    }

    public void removeOrderById(String orderId) {
        orderById.remove(orderId);
    }

    public Collection<Order> getOrders() {
        return orderById.values();
    }

    public BigDecimal getTotalSize() {
        BigDecimal totalSize = BigDecimal.ZERO;
        for (Order value : orderById.values()) {
            totalSize = totalSize.add(value.getSize());
        }
        return totalSize;
    }
}
