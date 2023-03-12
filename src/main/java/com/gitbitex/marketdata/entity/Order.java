package com.gitbitex.marketdata.entity;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Order {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private long sequence;
    private String productId;
    private String userId;
    private String clientOid;
    private Date time;
    private BigDecimal size;
    private BigDecimal funds;
    private BigDecimal filledSize;
    private BigDecimal executedValue;
    private BigDecimal price;
    private BigDecimal fillFees;
    private OrderType type;
    private OrderSide side;
    private OrderStatus status;
    /**
     * Time in force policies provide guarantees about the lifetime of an order. There are four policies: good till
     * canceled GTC, good till time GTT, immediate or cancel IOC, and fill or kill FOK.
     * <p>
     * GTC Good till canceled orders remain open on the book until canceled. This is the default behavior if no policy
     * is specified.
     * <p>
     * GTT Good till time orders remain open on the book until canceled or the allotted cancel_after is depleted on
     * the matching engine. GTT orders are guaranteed to cancel before any other order is processed after the
     * cancel_after timestamp which is returned by the API. A day is considered 24 hours.
     * <p>
     * IOC Immediate or cancel orders instantly cancel the remaining size of the limit order instead of opening it
     * on the book.
     * <p>
     * FOK Fill or kill orders are rejected if the entire size cannot be matched.
     */
    private String timeInForce;
    private boolean settled;
    /**
     * The post-only flag indicates that the order should only make liquidity. If any part of the order results in
     * taking liquidity, the order will be rejected and no part of it will execute.
     */
    private boolean postOnly;

}


