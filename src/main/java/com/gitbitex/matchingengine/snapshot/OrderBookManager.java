package com.gitbitex.matchingengine.snapshot;

import com.alibaba.fastjson.JSON;
import com.gitbitex.matchingengine.OrderBook;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

@Component
@Slf4j
public class OrderBookManager {
    private final RedissonClient redissonClient;
    private final RTopic l2BatchNotifyTopic;

    public OrderBookManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.l2BatchNotifyTopic = redissonClient.getTopic("l2_batch", StringCodec.INSTANCE);
    }

    public void saveOrderBook(String productId, byte[] bytes) {
        redissonClient.getBucket(keyForFull(productId), StringCodec.INSTANCE)
                .set(Base64.getEncoder().encodeToString(bytes));
    }

    @Nullable
    public OrderBook getOrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForFull(productId), StringCodec.INSTANCE).get();
        if (o == null) {
            return null;
        }
        return (OrderBook) SerializationUtils.deserialize(Base64.getDecoder().decode(o.toString()));
    }

    public void saveL3OrderBook(L3OrderBook l3OrderBook) {
        redissonClient.getBucket(keyForL3(l3OrderBook.getProductId())).set(JSON.toJSONString(l3OrderBook));
    }

    public L3OrderBook getL3OrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL3(productId)).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L3OrderBook.class);
    }

    public void saveL2OrderBook(L2OrderBook l2OrderBook) {
        redissonClient.getBucket(keyForL2(l2OrderBook.getProductId())).setAsync(JSON.toJSONString(l2OrderBook));
    }

    public L2OrderBook getL2OrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL2(productId)).get();
        if (o == null) {
            return null;
        }
        return JSON.parseObject(o.toString(), L2OrderBook.class);
    }

    public void saveL2BatchOrderBook(L2OrderBook l2OrderBook) {
        redissonClient.getBucket(keyForL2Batch(l2OrderBook.getProductId())).setAsync(JSON.toJSONString(l2OrderBook));
        l2BatchNotifyTopic.publishAsync(l2OrderBook.getProductId());
    }

    public L2OrderBook getL2BatchOrderBook(String productId) {
        Object o = redissonClient.getBucket(keyForL2Batch(productId)).get();
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

    public void saveL1OrderBook(L2OrderBook orderBook) {
        redissonClient.getBucket(keyForL1(orderBook.getProductId())).set(JSON.toJSONString(orderBook));
    }

    private String keyForL1(String productId) {
        return productId + ".l1_order_book";
    }

    private String keyForL2(String productId) {
        return productId + ".l2_order_book";
    }

    private String keyForL2Batch(String productId) {
        return productId + ".l2_batch_order_book";
    }

    private String keyForL3(String productId) {
        return productId + ".l3_order_book";
    }

    private String keyForFull(String productId) {
        return productId + ".full_order_book";
    }
}
