package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class User {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String twoStepVerificationType;
    private BigDecimal gotpSecret;
    private String nickName;
}
