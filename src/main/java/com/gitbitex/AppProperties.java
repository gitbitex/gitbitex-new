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
    private String matchingEngineCommandTopic;
    private String tradeMessageTopic;
    private String accountMessageTopic;
    private String orderMessageTopic;
    private int l2BatchOrderBookPersistenceInterval = 20;
    private int l2BatchOrderBookSize = 50;
    private int l2OrderBookPersistenceInterval = 10000;
    private int l3OrderBookPersistenceInterval = 10000;
}
