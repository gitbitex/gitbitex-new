package com.gitbitex.kafka;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class PendingOffsetManager {
    private final ConcurrentHashMap<TopicPartition, ConcurrentSkipListMap<Long, AtomicLong>> pendingOffsetsByPartition = new ConcurrentHashMap<>();

    public void remove(TopicPartition partition) {
        pendingOffsetsByPartition.remove(partition);
    }

    public void put(TopicPartition partition) {
        pendingOffsetsByPartition.put(partition, new ConcurrentSkipListMap<>());
    }

    public void retainOffset(TopicPartition partition, long offset) {
        pendingOffsetsByPartition.get(partition).compute(offset, (k, v) -> {
            if (v == null) {
                return new AtomicLong(1);
            }
            v.incrementAndGet();
            return v;
        });
    }

    public void releaseOffset(TopicPartition partition, long offset) {
        if (pendingOffsetsByPartition.containsKey(partition)) {
            pendingOffsetsByPartition.get(partition).computeIfPresent(offset, (k, v) -> {
                v.decrementAndGet();
                return v;
            });
        }
    }

    public long getPendingOffsetCount(TopicPartition topicPartition) {
        if (!pendingOffsetsByPartition.containsKey(topicPartition)) {
            return 0;
        }
        return pendingOffsetsByPartition.get(topicPartition).size();
    }


    public PartitionOffset getCommittableOffset(TopicPartition partition, int pendingOffsetSizeThreshold) {
        ConcurrentSkipListMap<Long, AtomicLong> pendingOffsets = pendingOffsetsByPartition.get(partition);
        if (pendingOffsets == null) {
            return null;
        }

        if (pendingOffsets.size() < pendingOffsetSizeThreshold) {
            return null;
        }

        long maxCommittableOffset = -1;
        for (Map.Entry<Long, AtomicLong> entry : pendingOffsets.entrySet()) {
            if (entry.getValue().get() > 0) {
                break;
            }
            maxCommittableOffset = entry.getKey();
        }

        if (maxCommittableOffset == -1) {
            return null;
        }

        pendingOffsets.headMap(maxCommittableOffset, true).clear();
        return new PartitionOffset(partition, new OffsetAndMetadata(maxCommittableOffset + 1));
    }

    public Map<TopicPartition, OffsetAndMetadata> commit(KafkaConsumer<?, ?> consumer, int pendingOffsetSizeThreshold) {
        Map<TopicPartition, OffsetAndMetadata> committableOffsets = new HashMap<>();
        pendingOffsetsByPartition.forEach((partition, pendingOffsets) -> {
            PartitionOffset committableOffset = getCommittableOffset(partition, pendingOffsetSizeThreshold);
            if (committableOffset != null) {
                committableOffsets.put(committableOffset.getTopicPartition(), committableOffset.getOffsetAndMetadata());
            }
        });

        if (!committableOffsets.isEmpty()) {
            consumer.commitSync(committableOffsets);
        }
        return committableOffsets;
    }

    public Map<TopicPartition, OffsetAndMetadata> commit(KafkaConsumer<?, ?> consumer) {
        return commit(consumer, 0);
    }

    public void commit(KafkaConsumer<?, ?> consumer, TopicPartition partition) {
        PartitionOffset offset = getCommittableOffset(partition, 0);
        if (offset != null) {
            consumer.commitSync(ImmutableMap.of(offset.getTopicPartition(), offset.getOffsetAndMetadata()));
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class PartitionOffset {
        private final TopicPartition topicPartition;
        private final OffsetAndMetadata offsetAndMetadata;
    }
}
