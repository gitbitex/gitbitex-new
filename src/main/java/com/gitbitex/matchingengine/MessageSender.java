package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.MessageSerializer;
import com.gitbitex.middleware.kafka.KafkaProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class MessageSender {
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final KafkaProducer<String, Message> kafkaProducer;

    public MessageSender(AppProperties appProperties, KafkaProperties kafkaProperties) {
        this.appProperties = appProperties;
        this.kafkaProperties = kafkaProperties;
        this.kafkaProducer = kafkaProducer();
    }

    public void send(Message message) {
        ProducerRecord<String, Message> record = new ProducerRecord<>(appProperties.getMatchingEngineMessageTopic(), message);
        kafkaProducer.send(record);
    }

    public KafkaProducer<String, Message> kafkaProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MessageSerializer.class.getName());
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
