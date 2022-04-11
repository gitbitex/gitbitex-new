package com.gitbitex.marketdata;

import com.alibaba.fastjson.JSON;

import com.gitbitex.marketdata.entity.Ticker;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TickerManager {
    private final RedissonClient redissonClient;

    public Ticker getTicker(String productId) {
        Object val = redissonClient.getBucket(productId + ".ticker", StringCodec.INSTANCE).get();
        if (val == null) {
            return null;
        }
        return JSON.parseObject(val.toString(), Ticker.class);
    }

    public void saveTicker(Ticker ticker) {
        redissonClient.getBucket(ticker.getProductId() + ".ticker", StringCodec.INSTANCE).set(
            JSON.toJSONString(ticker));
    }
}
