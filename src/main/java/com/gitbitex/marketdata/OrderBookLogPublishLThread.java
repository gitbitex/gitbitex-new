package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Collections;

@Slf4j
public class OrderBookLogPublishLThread extends KafkaConsumerThread<String, String> {
    private final String productId;
    private final AppProperties appProperties;
    private final RTopic logTopic;

    public OrderBookLogPublishLThread(String productId, KafkaConsumer<String, String> kafkaConsumer, RedissonClient redissonClient, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.appProperties = appProperties;
        this.logTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, String> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()));
    }

    @Override
    protected void processRecords(KafkaConsumer<String, String> consumer, ConsumerRecords<String, String> records) {
        for (ConsumerRecord<String, String> record : records) {
            logTopic.publish(record.value());
        }
    }
}
