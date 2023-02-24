package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.gitbitex.marketdata.entity.Order.OrderSide;
import lombok.Getter;

public class PageLine implements Serializable {
    @Getter
    private final BigDecimal price;
    private final LinkedHashMap<String, BookOrder> orderById = new LinkedHashMap<>();
    @Getter
    private final OrderSide side;

    public PageLine(BigDecimal price, OrderSide side) {
        this.price = price;
        this.side = side;
    }

    public void addOrder(BookOrder order) {
        orderById.put(order.getOrderId(), order);
    }

    public void decreaseOrderSize(String orderId, BigDecimal size) {
        BookOrder order = orderById.get(orderId);
        order.setSize(order.getSize().subtract(size));
    }

    public void removeOrderById(String orderId) {
        orderById.remove(orderId);
    }

    public Collection<BookOrder> getOrders() {
        return orderById.values();
    }

    public BigDecimal getTotalSize() {
        BigDecimal totalSize = BigDecimal.ZERO;
        for (BookOrder value : orderById.values()) {
            totalSize = totalSize.add(value.getSize());
        }
        return totalSize;
    }
}
