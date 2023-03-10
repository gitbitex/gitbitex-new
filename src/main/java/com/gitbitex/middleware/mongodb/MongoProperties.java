package com.gitbitex.middleware.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "mongodb")
@Getter
@Setter
@Validated
public class MongoProperties {
    private String uri;
}
