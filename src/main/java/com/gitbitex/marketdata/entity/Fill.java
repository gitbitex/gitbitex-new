package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.gitbitex.marketdata.enums.OrderSide;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
public class Fill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    private String fillId;

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
