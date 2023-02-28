package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trade {
    private String productId;
    private long tradeId;
    private BigDecimal size;
    private BigDecimal funds;
    private BigDecimal price;
    private Date time;
    private OrderSide side;
    private String takerOrderId;
    private String makerOrderId;

}
