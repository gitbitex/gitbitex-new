package com.gitbitex.matchingengine.snapshot;

import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSON;

import com.gitbitex.matchingengine.OrderBook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderBookSnapshotManager {
    private final RedissonClient redissonClient;

    public void saveOrderBookSnapshot(String productId, byte[] bytes) {
        String key = productId + ".order_book_snapshot";
        redissonClient.getBucket(key).set(new String(bytes));
    }

    @Nullable
    public OrderBook getOrderBookSnapshot(String productId) {
        String key = productId + ".order_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return (OrderBook)SerializationUtils.deserialize(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void saveLevel3BookSnapshot(String productId, L3OrderBookSnapshot snapshot) {
        String key = productId + ".level3_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(snapshot));
    }

    public L3OrderBookSnapshot getLevel3Snapshot(String productId) {
        String key = productId + ".level3_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L3OrderBookSnapshot.class);
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
