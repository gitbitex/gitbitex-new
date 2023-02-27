package com.gitbitex;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "gbe")
@Getter
@Setter
@Validated
public class AppProperties {
    private String tradeMessageTopic;

    private String accountMessageTopic;

    private String orderMessageTopic;

    private String matchingEngineCommandTopic;

    private String orderBookMessageTopic;

    private int accountantThreadNum;

    private int orderProcessorThreadNum;

    private int l2BatchOrderBookPersistenceInterval = 20;

    private int l2BatchOrderBookSize = 50;

    private int l2OrderBookPersistenceInterval = 10000;

    private int l3OrderBookPersistenceInterval = 10000;

    private int fullOrderBookPersistenceInterval = 30000;

    private int fullOrderBookPersistenceThreshold = 10000;

    private Set<String> liquidityTraderUserIds;
}
