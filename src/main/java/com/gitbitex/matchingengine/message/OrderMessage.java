package com.gitbitex.matchingengine.message;

import com.gitbitex.matchingengine.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderMessage extends Order {
    private BigDecimal fillFees;
    private BigDecimal filledSize;
    private BigDecimal executedValue;
}
