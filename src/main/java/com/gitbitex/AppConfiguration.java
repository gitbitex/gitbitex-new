package com.gitbitex;

import java.util.Properties;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.support.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class AppConfiguration {

    @Bean
    public KafkaMessageProducer kafkaMessageProducer(AppProperties appProperties, KafkaProperties kafkaProperties) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put("compression.type", "zstd");
        properties.put("max.in.flight.requests.per.connection", 1);
        properties.put("reties", 2147483647);
        return new KafkaMessageProducer(properties, appProperties);
    }
}



