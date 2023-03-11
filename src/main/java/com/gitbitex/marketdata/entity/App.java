package com.gitbitex.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class App {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String userId;
    private String name;
    private String accessKey;
    private String secretKey;
}
