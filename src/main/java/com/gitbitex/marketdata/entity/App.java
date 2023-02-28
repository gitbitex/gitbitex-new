package com.gitbitex.marketdata.entity;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class App {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String appId;
    private String userId;
    private String name;
    private String accessKey;
    private String secretKey;
}
