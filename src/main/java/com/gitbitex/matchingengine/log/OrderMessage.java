package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;

import com.gitbitex.matchingengine.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessage extends Order {
    private BigDecimal fillFees;
    private BigDecimal filledSize;
    private BigDecimal executedValue;
}
