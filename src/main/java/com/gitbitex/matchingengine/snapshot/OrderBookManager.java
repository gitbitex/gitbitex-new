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
public class OrderBookManager {
    private final RedissonClient redissonClient;

    public void saveOrderBook(String productId, byte[] bytes) {
        redissonClient.getBucket(keyForFull(productId)).set(new String(bytes));
    }

    @Nullable
    public OrderBook getOrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForFull(productId)).get();
        if (o == null) {
            return null;
        }
        return (OrderBook)SerializationUtils.deserialize(o.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void saveL3OrderBook(String productId, L3OrderBook l3OrderBook) {
        redissonClient.getBucket(keyForL3(productId)).set(JSON.toJSONString(l3OrderBook));
    }

    public L3OrderBook getL3OrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL3(productId)).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L3OrderBook.class);
    }

    public void saveL2OrderBook(String productId, L2OrderBook l2OrderBook) {
        redissonClient.getBucket(keyForL2(productId)).set(JSON.toJSONString(l2OrderBook));
    }

    public L2OrderBook getL2OrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL2(productId)).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBook.class);
    }

    public L2OrderBook getL1OrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL1(productId)).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBook.class);
    }

    public void saveL1OrderBook(String productId, L2OrderBook orderBook) {
        redissonClient.getBucket(keyForL1(productId)).set(JSON.toJSONString(orderBook));
    }

    private String keyForL1(String productId) {
        return productId + ".l1_order_book";
    }

    private String keyForL2(String productId) {
        return productId + ".l2_order_book";
    }

    private String keyForL3(String productId) {
        return productId + ".l3_order_book";
    }

    private String keyForFull(String productId) {
        return productId + ".full_order_book";
    }
}
