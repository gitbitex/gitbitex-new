package com.gitbitex.user.entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
public class User {

    private Date createdAt;

    private Date updatedAt;

    @Id
    private String userId;

    private String email;

    private String passwordHash;

    private String passwordSalt;

    private String twoStepVerificationType;

    private BigDecimal gotpSecret;

    private String nickName;
}
