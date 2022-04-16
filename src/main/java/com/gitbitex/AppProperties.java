package com.gitbitex;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

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

    private int l2BatchOrderBookPersistenceInterval = 20;

    private int l2BatchOrderBookSize = 50;

    private int l2OrderBookPersistenceInterval = 10000;

    private int l3OrderBookPersistenceInterval = 10000;

    private int fullOrderBookPersistenceInterval = 30000;

    private int fullOrderBookPersistenceThreshold = 10000;

    private Set<String> liquidityTraderUserIds;
}
