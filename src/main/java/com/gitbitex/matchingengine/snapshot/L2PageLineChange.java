package com.gitbitex.matchingengine.snapshot;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2PageLineChange extends ArrayList<String> {
    public L2PageLineChange(){}

    public L2PageLineChange(OrderSide side, BigDecimal price, BigDecimal size) {
        this.add(side.name().toLowerCase());
        this.add(price.stripTrailingZeros().toPlainString());
        this.add(size.stripTrailingZeros().toPlainString());
    }
}
