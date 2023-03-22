package com.gitbitex.matchingengine;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Getter
public class PriceGroupedOrderCollection extends LinkedHashMap<String, Order> {
    //public BigDecimal remainingSize = BigDecimal.ZERO;

    public void addOrder(Order order) {
        put(order.getId(), order);
        //remainingSize = remainingSize.add(order.getRemainingSize());
    }

    public void decrRemainingSize(BigDecimal size) {
        //remainingSize=remainingSize.subtract(size);
    }

    public BigDecimal getRemainingSize() {
        return values().stream()
                .map(Order::getRemainingSize)
                .reduce(BigDecimal::add)
                .get();
    }
}
