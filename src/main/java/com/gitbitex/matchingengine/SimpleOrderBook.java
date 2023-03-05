package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;

@Getter
public class SimpleOrderBook {
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> asks = new TreeMap<>(Comparator.naturalOrder());
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> bids = new TreeMap<>(Comparator.reverseOrder());
    private String productId;

    public void putOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupOrderCollection> page = (order.getSide() == OrderSide.BUY ? bids : asks);
        PriceGroupOrderCollection priceGroupOrderCollection = page.computeIfAbsent(order.getPrice(),
                k -> new PriceGroupOrderCollection());
        Order old= priceGroupOrderCollection.get(order.getOrderId());
        if (old!=null){
            BigDecimal diff= old.getRemainingSize().subtract(order.getRemainingSize());
            priceGroupOrderCollection.decrRemainingSize(diff);
        }
        priceGroupOrderCollection.addOrder(order);
    }

    public void removeOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupOrderCollection> page = (order.getSide() == OrderSide.BUY ? bids : asks);
        PriceGroupOrderCollection priceGroupOrderCollection = page
            .computeIfPresent(order.getPrice(), (k, v) -> {
                v.remove(order.getOrderId());
                return v;
            });
        if (priceGroupOrderCollection != null) {
            priceGroupOrderCollection.decrRemainingSize(order.getRemainingSize());
        }
        if (priceGroupOrderCollection != null && priceGroupOrderCollection.isEmpty()) {
            page.remove(order.getPrice());
        }
    }
}
