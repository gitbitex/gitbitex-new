package com.gitbitex.matchingengine.log;

import java.math.BigDecimal;
import java.util.Date;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeMessage extends Log {
    private long tradeId;

    private String productId;

    private String takerOrderId;

    private String makerOrderId;

    private BigDecimal price;

    private BigDecimal size;

    private OrderSide side;

    private Date time;

    public TradeMessage() {
        this.setType(LogType.TRADE);
    }
}
