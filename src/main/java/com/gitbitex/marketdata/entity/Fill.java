package com.gitbitex.marketdata.entity;

import com.gitbitex.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Fill {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String orderId;
    private long tradeId;
    private String productId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private BigDecimal fee;
    private String liquidity;
    private boolean settled;
    private OrderSide side;
    private boolean done;
    private String doneReason;
}
