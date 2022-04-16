package com.gitbitex.matchingengine;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;

public class PageLine implements Serializable {
    @Getter
    private final BigDecimal price;
    private final LinkedHashMap<String, BookOrder> orderById = new LinkedHashMap<>();
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
        orderById.put(order.getOrderId(), order);
    }

    public void decreaseOrderSize(String orderId, BigDecimal size) {
        BookOrder order = orderById.get(orderId);
        order.setSize(order.getSize().subtract(size));
        totalSize = totalSize.subtract(size);
    }

    public void removeOrderById(String orderId) {
        BookOrder order = orderById.remove(orderId);
        if (order != null) {
            totalSize = totalSize.subtract(order.getSize());
        }
    }

    public Collection<BookOrder> getOrderById() {
        return orderById.values();
    }
}
