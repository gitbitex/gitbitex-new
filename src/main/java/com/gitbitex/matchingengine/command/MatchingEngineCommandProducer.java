package com.gitbitex.matchingengine.command;

import com.gitbitex.AppProperties;
import com.gitbitex.middleware.kafka.KafkaProperties;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class MatchingEngineCommandProducer {
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final KafkaProducer<String, Command> kafkaProducer;

    public MatchingEngineCommandProducer(AppProperties appProperties, KafkaProperties kafkaProperties) {
        this.appProperties = appProperties;
        this.kafkaProperties = kafkaProperties;
        this.kafkaProducer = kafkaProducer();
    }

    public void send(Command command, Callback callback) {
        ProducerRecord<String, Command> record = new ProducerRecord<>(appProperties.getMatchingEngineCommandTopic(), command);
        kafkaProducer.send(record, callback);
    }

    public KafkaProducer<String, Command> kafkaProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CommandSerializer.class.getName());
        properties.put("compression.type", "zstd");
        properties.put("retries", 2147483647);
        properties.put("linger.ms", 100);
        properties.put("batch.size", 16384 * 2);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); //Important, prevent message duplication
        properties.put("max.in.flight.requests.per.connection", 5); // Must be less than or equal to 5
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(properties);
    }
}
