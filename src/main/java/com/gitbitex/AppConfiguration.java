package com.gitbitex;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.support.kafka.KafkaProperties;
import com.gitbitex.support.metric.JsonSlf4Reporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
@Slf4j
public class AppConfiguration {

    @Bean
    public KafkaMessageProducer kafkaMessageProducer(AppProperties appProperties, KafkaProperties kafkaProperties, MetricRegistry metricRegistry) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put("compression.type", "zstd");
        properties.put("max.in.flight.requests.per.connection", 1);
        properties.put("retries", 2147483647);
        return new KafkaMessageProducer(properties, appProperties, metricRegistry);
    }

    @Bean
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

    @Bean
    public JsonSlf4Reporter JsonSlf4Reporter(MetricRegistry metricRegistry) {
        return new JsonSlf4Reporter(metricRegistry, "default", MetricFilter.ALL, TimeUnit.SECONDS,
                TimeUnit.MILLISECONDS, LoggerFactory.getLogger("gbeMetric"));
    }
}



