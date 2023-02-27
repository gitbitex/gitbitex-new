package com.gitbitex.user.entity;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
public class App {
    @Id
    private long id;

    private Date createdAt;

    private Date updatedAt;

    private String appId;

    private String userId;

    private String name;

    private String accessKey;

    private String secretKey;
}
