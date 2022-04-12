package com.gitbitex.matchingengine.snapshot;

import java.nio.charset.StandardCharsets;

import javax.validation.constraints.Null;

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

    public void saveLevel3BookSnapshot(String productId, L3OrderBook snapshot) {
        String key = productId + ".level3_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(snapshot));
    }

    public L3OrderBook getLevel3Snapshot(String productId) {
        String key = productId + ".level3_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L3OrderBook.class);
    }

    public void saveLevel2BookSnapshot(String productId, L2OrderBook l2OrderBook) {
        String key = productId + ".level2_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(l2OrderBook));
    }

    public L2OrderBook getLevel2BookSnapshot(String productId) {
        String key = productId + ".level2_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBook.class);
    }

    public L2OrderBook getLevel1BookSnapshot(String productId) {
        String key = productId + ".level1_book_snapshot";
        Object o = redissonClient.getBucket(key).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBook.class);
    }

    public void saveLevel1BookSnapshot(String productId, L2OrderBook level1OrderBookSnapshot) {
        String key = productId + ".level1_book_snapshot";
        redissonClient.getBucket(key).set(JSON.toJSONString(level1OrderBookSnapshot));
    }
}
