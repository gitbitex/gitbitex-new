package com.gitbitex.matchingengine;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class MessageConsumerThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    protected final Map<TopicPartition, OffsetAndMetadata> allOffsets = new HashMap<>();
    protected final Map<TopicPartition, Long> lastMessageSequences = new HashMap<>();
    private final AppProperties appProperties;
    private final Logger logger;

    public MessageConsumerThread(KafkaConsumer<String, Message> consumer, AppProperties appProperties, Logger logger) {
        super(consumer, logger);
        this.appProperties = appProperties;
        this.logger = logger;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }

        var offsets = consumer.committed(Sets.newHashSet(partitions));
        offsets.forEach((k, v) -> {
            if (v != null) {
                String metadata = v.metadata();
                if (StringUtils.isNotBlank(metadata)) {
                    lastMessageSequences.put(k, Long.parseLong(metadata));
                }
            }
        });
        allOffsets.putAll(offsets);
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        processRecords(records);

        // commit
        records.forEach(x -> allOffsets.put(new TopicPartition(x.topic(), x.partition()),
                new OffsetAndMetadata(x.offset() + 1, String.valueOf(x.value().getSequence()))));
        consumer.commitSync(allOffsets);
    }

    protected void checkMessageSequence(TopicPartition topicPartition, Message message) {
        Long lastSeq = lastMessageSequences.get(topicPartition);
        if (lastSeq != null) {
            if (message.getSequence() <= lastSeq) {
                logger.warn("drop repeat message, sequence={}", message.getSequence());
                return;
            } else if (message.getSequence() != lastSeq + 1) {
                throw new RuntimeException("out of sequence, sequence=" + message.getSequence());
            }
        }
        lastMessageSequences.put(topicPartition, message.getSequence());
    }

    protected abstract void processRecords(ConsumerRecords<String, Message> records);
}
