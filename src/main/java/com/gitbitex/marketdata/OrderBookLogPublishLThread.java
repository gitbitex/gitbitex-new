package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public class OrderBookLogPublishLThread extends KafkaConsumerThread<String, String> {
    private final String productId;
    private final AppProperties appProperties;
    private final RTopic logTopic;
    private long uncommittedRecordCount;

    public OrderBookLogPublishLThread(String productId, KafkaConsumer<String, String> kafkaConsumer,
                                      RedissonClient redissonClient, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productId = productId;
        this.appProperties = appProperties;
        this.logTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                consumer.commitSync();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {

            }
        });
    }

    @Override
    protected void processRecords(ConsumerRecords<String, String> records) {
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, String> record : records) {
            logTopic.publishAsync(record.value());
        }

        if (uncommittedRecordCount > 10000) {
            consumer.commitAsync();
            uncommittedRecordCount = 0;
        }
    }
}
