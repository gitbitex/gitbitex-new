package com.gitbitex;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.middleware.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
public class AppConfiguration {

    @Bean(destroyMethod = "close")
    public KafkaMessageProducer kafkaMessageProducer(AppProperties appProperties, KafkaProperties kafkaProperties) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put("compression.type", "zstd");
        properties.put("max.in.flight.requests.per.connection", 1);
        properties.put("retries", 2147483647);
        properties.put("linger.ms", 100);
        properties.put("batch.size", 16384 * 2);
        return new KafkaMessageProducer(properties, appProperties);
    }

}



