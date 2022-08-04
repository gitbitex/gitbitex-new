package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class OrderBookLogPublishThread extends KafkaConsumerThread<String, String>
    implements ConsumerRebalanceListener {
    private final List<String> productIds;
    private final AppProperties appProperties;
    private final RTopic logTopic;
    private long uncommittedRecordCount;

    public OrderBookLogPublishThread(List<String> productIds, KafkaConsumer<String, String> kafkaConsumer,
        RedissonClient redissonClient, AppProperties appProperties) {
        super(kafkaConsumer, logger);
        this.productIds = productIds;
        this.appProperties = appProperties;
        this.logTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync();

        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }
    }

    @Override
    protected void doSubscribe() {
        List<String> topics = productIds.stream()
            .map(x -> TopicUtil.getProductTopic(x, appProperties.getOrderBookLogTopic()))
            .collect(Collectors.toList());
        consumer.subscribe(topics, this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, String> record : records) {
            logTopic.publishAsync(record.value());
        }

        if (uncommittedRecordCount > 10000) {
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }
}
