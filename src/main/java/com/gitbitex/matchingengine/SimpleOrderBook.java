package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
public class SimpleOrderBook {
    private final String productId;
    private final TreeMap<BigDecimal, PriceGroupedOrderCollection> asks = new TreeMap<>(Comparator.naturalOrder());
    private final TreeMap<BigDecimal, PriceGroupedOrderCollection> bids = new TreeMap<>(Comparator.reverseOrder());
    @Setter
    private long sequence;

    public SimpleOrderBook(String productId) {
        this.productId = productId;
    }

    public SimpleOrderBook(String productId, long sequence) {
        this.productId = productId;
        this.sequence = sequence;
    }

    public void putOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupedOrderCollection> ordersByPrice = (order.getSide() == OrderSide.BUY ? bids
            : asks);
        PriceGroupedOrderCollection priceGroupedOrders = ordersByPrice.get(order.getPrice());
        if (priceGroupedOrders == null) {
            priceGroupedOrders = new PriceGroupedOrderCollection();
            ordersByPrice.put(order.getPrice(), priceGroupedOrders);
        }
        Order old = priceGroupedOrders.get(order.getOrderId());
        if (old != null) {
            BigDecimal diff = old.getRemainingSize().subtract(order.getRemainingSize());
            priceGroupedOrders.decrRemainingSize(diff);
        }
        priceGroupedOrders.addOrder(order);
    }

    public void removeOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupedOrderCollection> ordersByPrice = (order.getSide() == OrderSide.BUY ? bids
            : asks);
        PriceGroupedOrderCollection priceGroupedOrders = ordersByPrice.get(order.getPrice());
        if (priceGroupedOrders == null) {
            return;
        }
        priceGroupedOrders.remove(order.getOrderId());
        priceGroupedOrders.decrRemainingSize(order.getRemainingSize());

        if (priceGroupedOrders.isEmpty()) {
            ordersByPrice.remove(order.getPrice());
        }
    }
}
