package com.gitbitex.marketdata.manager;

import com.alibaba.fastjson.JSON;
import com.gitbitex.marketdata.entity.Ticker;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
public class TickerManager {
    private final RedissonClient redissonClient;
    private final RTopic tickerTopic;

    public TickerManager(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.tickerTopic = redissonClient.getTopic("ticker", StringCodec.INSTANCE);
    }

    public Ticker getTicker(String productId) {
        Object val = redissonClient.getBucket(keyForTicker(productId), StringCodec.INSTANCE).get();
        if (val == null) {
            return null;
        }
        return JSON.parseObject(val.toString(), Ticker.class);
    }

    public void saveTicker(Ticker ticker) {
        String value = JSON.toJSONString(ticker);
        redissonClient.getBucket(keyForTicker(ticker.getProductId()), StringCodec.INSTANCE).set(value);
        tickerTopic.publishAsync(value);
    }

    private String keyForTicker(String productId) {
        return productId + ".ticker";
    }
}
