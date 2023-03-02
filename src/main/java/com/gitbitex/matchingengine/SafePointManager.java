package com.gitbitex.matchingengine;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Component;

@Component
public class SafePointManager {
    private final RBucket<Long> safePointBucket;

    public SafePointManager(RedissonClient redissonClient) {
        safePointBucket = redissonClient.getBucket("safePoint.commandOffset", LongCodec.INSTANCE);
    }

    public void putCommandOffset(Long commandOffset) {
        safePointBucket.set(commandOffset);
    }

    public Long getCommandOffset() {
        return safePointBucket.get();
    }
}
