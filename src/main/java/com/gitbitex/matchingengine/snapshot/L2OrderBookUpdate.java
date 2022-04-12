package com.gitbitex.matchingengine.snapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2OrderBookUpdate {
    private String productId;
    private Collection<Change> changes;

    public static class Change extends ArrayList<String> {
        public Change() {}

        public Change(OrderSide side, BigDecimal price, BigDecimal size) {
            this.add(side.name().toLowerCase());
            this.add(price.stripTrailingZeros().toPlainString());
            this.add(size.stripTrailingZeros().toPlainString());
        }
    }
}
