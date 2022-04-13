package com.gitbitex.order.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "`order`")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    private String orderId;

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

    @Enumerated(EnumType.STRING)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
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

    public enum OrderType {
        /**
         * limit price order
         */
        LIMIT,
        /**
         * market price order
         */
        MARKET,
    }

    public enum OrderSide {
        /**
         * buy
         */
        BUY,
        /**
         * sell
         */
        SELL;

        /**
         * opposite
         *
         * @return
         */
        public OrderSide opposite() {
            return this == BUY ? SELL : BUY;
        }
    }

    public enum OrderStatus {
        NEW,
        RECEIVED,
        OPEN,
        CANCELLED,
        FILLED,
        DENIED,
    }

    public enum TimeInForcePolicy {
        GTC,
        GTT,
        IOC,
        FOK,
    }
}


