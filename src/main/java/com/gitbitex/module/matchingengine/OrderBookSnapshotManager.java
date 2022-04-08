package com.gitbitex.module.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.module.matchingengine.marketmessage.Level2OrderBookSnapshot;
import com.gitbitex.module.matchingengine.marketmessage.Level3OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBookSnapshotManager {
    private final RedissonClient redissonClient;

    public void saveLevel3BookSnapshot(String productId, Level3OrderBookSnapshot snapshot) {
        String key = productId + ".level3_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(snapshot));
    }

    public Level3OrderBookSnapshot getLevel3Snapshot(String productId) {
        String key = productId + ".level3_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), Level3OrderBookSnapshot.class);
    }

    public void saveLevel2BookSnapshot(String productId, Level2OrderBookSnapshot level2OrderBookSnapshot) {
        String key = productId + ".level2_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(level2OrderBookSnapshot));
    }

    public Level2OrderBookSnapshot getLevel2BookSnapshot(String productId) {
        String key = productId + ".level2_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), Level2OrderBookSnapshot.class);
    }

    public Level2OrderBookSnapshot getLevel1BookSnapshot(String productId) {
        String key = productId + ".level1_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), Level2OrderBookSnapshot.class);
    }

    public void saveLevel1BookSnapshot(String productId, Level2OrderBookSnapshot level1OrderBookSnapshot) {
        String key = productId + ".level1_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(level1OrderBookSnapshot));
    }
}
