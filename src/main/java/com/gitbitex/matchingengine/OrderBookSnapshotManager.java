package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.matchingengine.marketmessage.L2OrderBookSnapshot;
import com.gitbitex.matchingengine.marketmessage.Level3OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBookSnapshotManager {
    private final RedissonClient redissonClient;

    public void saveOrderBookSnapshot(String productId, OrderBookSnapshot snapshot) {
        String key = productId + ".order_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(snapshot));
    }

    public OrderBookSnapshot getOrderBookSnapshot(String productId) {
        String key = productId + ".order_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), OrderBookSnapshot.class);
    }

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

    public void saveLevel2BookSnapshot(String productId, L2OrderBookSnapshot l2OrderBookSnapshot) {
        String key = productId + ".level2_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(l2OrderBookSnapshot));
    }

    public L2OrderBookSnapshot getLevel2BookSnapshot(String productId) {
        String key = productId + ".level2_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBookSnapshot.class);
    }

    public L2OrderBookSnapshot getLevel1BookSnapshot(String productId) {
        String key = productId + ".level1_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBookSnapshot.class);
    }

    public void saveLevel1BookSnapshot(String productId, L2OrderBookSnapshot level1OrderBookSnapshot) {
        String key = productId + ".level1_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(level1OrderBookSnapshot));
    }
}
