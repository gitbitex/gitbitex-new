package com.gitbitex.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

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
