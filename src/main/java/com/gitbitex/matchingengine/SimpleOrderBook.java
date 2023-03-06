package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

@Getter
public class SimpleOrderBook {
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> asks = new TreeMap<>(Comparator.naturalOrder());
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> bids = new TreeMap<>(Comparator.reverseOrder());
    private final String productId;
    private long version;
    @Setter
    private long sequence;

    public SimpleOrderBook(String productId) {
        this.productId = productId;
    }

    public SimpleOrderBook(String productId,long sequence) {
        this.productId = productId;
        this.sequence=sequence;
    }

    public void putOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupOrderCollection> page = (order.getSide() == OrderSide.BUY ? bids : asks);
        PriceGroupOrderCollection priceGroupOrderCollection = page.get(order.getPrice());
        if (priceGroupOrderCollection==null){
            priceGroupOrderCollection=new PriceGroupOrderCollection();
            page.put(order.getPrice(),priceGroupOrderCollection);
        }
        Order old = priceGroupOrderCollection.get(order.getOrderId());
        if (old != null) {
            BigDecimal diff = old.getRemainingSize().subtract(order.getRemainingSize());
            priceGroupOrderCollection.decrRemainingSize(diff);
        }
        priceGroupOrderCollection.addOrder(order);
    }

    public void removeOrder(Order order) {
        TreeMap<BigDecimal, PriceGroupOrderCollection> page = (order.getSide() == OrderSide.BUY ? bids : asks);
        PriceGroupOrderCollection priceGroupOrderCollection = page.get(order.getPrice());
        if (priceGroupOrderCollection == null) {
            return;
        }
        priceGroupOrderCollection.remove(order.getOrderId());
        priceGroupOrderCollection.decrRemainingSize(order.getRemainingSize());

        if (priceGroupOrderCollection.isEmpty()) {
            page.remove(order.getPrice());
        }
    }
}
