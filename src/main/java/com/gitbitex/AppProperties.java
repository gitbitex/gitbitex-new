package com.gitbitex;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gbe")
@Getter
@Setter
@Validated
public class AppProperties {
    private String accountCommandTopic;

    private String orderCommandTopic;

    private String orderBookCommandTopic;

    private String orderBookLogTopic;

    private int accountantThreadNum;

    private int orderProcessorThreadNum;
}
