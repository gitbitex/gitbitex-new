package com.gitbitex.feedserver;

import com.alibaba.fastjson.JSON;
import com.gitbitex.feedserver.message.AccountMessage;
import com.gitbitex.feedserver.message.OrderMessage;
import com.gitbitex.matchingengine.marketmessage.CandleMessage;
import com.gitbitex.matchingengine.marketmessage.Level2UpdateMessage;
import com.gitbitex.matchingengine.marketmessage.MatchMessage;
import com.gitbitex.matchingengine.marketmessage.TickerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class FeedMessageListener {
    private final RedissonClient redissonClient;
    private final SessionManager sessionManager;

    @PostConstruct
    public void run() {
        redissonClient.getTopic("match", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            MatchMessage message = JSON.parseObject(msg, MatchMessage.class);
            String channel = message.getProductId() + "." + message.getType();
            sessionManager.sendMessageToChannel(channel, msg);
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            TickerMessage message = JSON.parseObject(msg, TickerMessage.class);
            String channel = message.getProductId() + "." + message.getType();
            sessionManager.sendMessageToChannel(channel, msg);
        });

        redissonClient.getTopic("candle", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            CandleMessage message = JSON.parseObject(msg, CandleMessage.class);
            String channel = message.getProductId() + "." + message.getType() + "_" + message.getGranularity() * 60;
            sessionManager.sendMessageToChannel(channel, msg);
        });

        redissonClient.getTopic("l2update", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Level2UpdateMessage message = JSON.parseObject(msg, Level2UpdateMessage.class);
            String channel = message.getProductId() + ".level2";
            sessionManager.sendMessageToChannel(channel, msg);
        });

        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            OrderMessage message = JSON.parseObject(msg, OrderMessage.class);
            String channel = message.getUserId() + "." + message.getProductId() + "." + message.getType();
            sessionManager.sendMessageToChannel(channel, msg);
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            AccountMessage message = JSON.parseObject(msg, AccountMessage.class);
            String channel = message.getUserId() + "." + message.getCurrencyCode() + ".funds";
            sessionManager.sendMessageToChannel(channel, msg);
        });

    }

}
