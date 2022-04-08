package com.gitbitex.module.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.gitbitex.module.order.entity.Order;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @CreationTimestamp
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    private long tradeId;

    private String productId;

    private String takerOrderId;

    private String makerOrderId;

    private BigDecimal price;

    private BigDecimal size;

    @Enumerated(EnumType.STRING)
    private Order.OrderSide side;

    private Date time;
}
