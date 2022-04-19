package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppDto {
    private String id;
    private String name;
    private String key;
    private String secret;
    private String createdAt;
}
