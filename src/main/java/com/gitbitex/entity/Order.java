package com.gitbitex.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

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

    private String timeInForce;

    private boolean settled;

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
}


