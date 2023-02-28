package com.gitbitex.marketdata.entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private Date createdAt;

    private Date updatedAt;

    private String userId;

    private String email;

    private String passwordHash;

    private String passwordSalt;

    private String twoStepVerificationType;

    private BigDecimal gotpSecret;

    private String nickName;
}
