package com.gitbitex.module.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.module.matchingengine.marketmessage.TickerMessage;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TickerManager {
    private final RedissonClient redissonClient;

    public TickerMessage getTicker(String productId) {
        Object val = redissonClient.getBucket(productId + ".ticker", StringCodec.INSTANCE).get();
        if (val == null) {
            return null;
        }
        return JSON.parseObject(val.toString(), TickerMessage.class);
    }

    public void setTicker(String productId, TickerMessage tickerMessage) {
        redissonClient.getBucket(productId + ".ticker", StringCodec.INSTANCE).set(JSON.toJSONString(tickerMessage));
    }
}
