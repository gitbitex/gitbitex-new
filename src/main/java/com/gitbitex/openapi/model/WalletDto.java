package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class WalletDto {
    private String id;
    private String userId;
    private String currency;
    private String address;
    private String balance;
    private String locked;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}
