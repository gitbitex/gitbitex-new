package com.gitbitex.support.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "kafka")
@Getter
@Setter
@Validated
public class KafkaProperties {
    private String bootstrapServers;
}
