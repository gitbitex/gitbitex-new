package com.gitbitex.matchingengine;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;

public class PageLine {
    @Getter
    private final BigDecimal price;
    private final LinkedHashMap<String, BookOrder> orders = new LinkedHashMap<>();
    @Getter
    private final OrderSide side;
    @Getter
    private BigDecimal totalSize = BigDecimal.ZERO;

    public PageLine(BigDecimal price, OrderSide side) {
        this.price = price;
        this.side = side;
    }

    public void addOrder(BookOrder order) {
        totalSize = totalSize.add(order.getSize());
        orders.put(order.getOrderId(), order);
    }

    public void decreaseOrderSize(String orderId, BigDecimal size) {
        BookOrder order = orders.get(orderId);
        order.setSize(order.getSize().subtract(size));
        totalSize = totalSize.subtract(size);
    }

    public void removeOrderById(String orderId) {
        BookOrder order = orders.remove(orderId);
        if (order != null) {
            totalSize = totalSize.subtract(order.getSize());
        }
    }

    public Collection<BookOrder> getOrders() {
        return orders.values();
    }
}
