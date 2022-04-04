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
    private String accountCommandTopic = "account_command1";

    private String orderBookCommandTopic = "order_book_command1";

    private String orderBookLogTopic = "order_book_log1";

    private String orderCommandTopic = "order_command1";
}
