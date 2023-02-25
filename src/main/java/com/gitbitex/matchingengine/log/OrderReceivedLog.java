package com.gitbitex.matchingengine.log;

import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.enums.OrderSide;
import com.gitbitex.marketdata.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class OrderReceivedLog extends Log {
    private String productId;
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private OrderSide side;
    private OrderType orderType;
    private String clientOid;
    private Date time;

    public OrderReceivedLog() {
        this.setType(LogType.ORDER_RECEIVED);
    }
}

