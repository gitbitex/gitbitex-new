package com.gitbitex.feed;

import java.util.Collections;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;

import com.gitbitex.account.entity.Account;
import com.gitbitex.feed.message.AccountMessage;
import com.gitbitex.feed.message.CandleMessage;
import com.gitbitex.feed.message.L2UpdateMessage;
import com.gitbitex.feed.message.MatchMessage;
import com.gitbitex.feed.message.OrderMessage;
import com.gitbitex.feed.message.TickerMessage;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.matchingengine.marketmessage.L2Change;
import com.gitbitex.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FeedMessageListener {
    private final RedissonClient redissonClient;
    private final SessionManager sessionManager;

    @PostConstruct
    public void run() {
        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Order order = JSON.parseObject(msg, Order.class);
            String channel = order.getUserId() + "." + order.getProductId() + ".order";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(orderMessage(order)));
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Account account = JSON.parseObject(msg, Account.class);
            String channel = account.getUserId() + "." + account.getCurrency() + ".funds";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(accountMessage(account)));
        });

        redissonClient.getTopic("trade", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Trade trade = JSON.parseObject(msg, Trade.class);
            String channel = trade.getProductId() + ".match";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(matchMessage(trade)));
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Ticker ticker = JSON.parseObject(msg, Ticker.class);
            String channel = ticker.getProductId() + ".ticker";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(new TickerMessage(ticker)));
        });

        redissonClient.getTopic("candle", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Candle candle = JSON.parseObject(msg, Candle.class);
            String channel = candle.getProductId() + ".candle_" + candle.getGranularity() * 60;
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(candleMessage(candle)));
        });

        redissonClient.getTopic("l2change", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            L2Change change = JSON.parseObject(msg, L2Change.class);
            String channel = change.getProductId() + ".level2";
            sessionManager.sendMessageToChannel(channel,
                JSON.toJSONString(new L2UpdateMessage(change.getProductId(), Collections.singletonList(change))));
        });
    }

    private MatchMessage matchMessage(Trade trade) {
        MatchMessage message = new MatchMessage();
        message.setTradeId(trade.getTradeId());
        message.setSequence(trade.getSequence());
        message.setTakerOrderId(trade.getTakerOrderId());
        message.setMakerOrderId(trade.getMakerOrderId());
        message.setTime(trade.getTime().toInstant().toString());
        message.setProductId(trade.getProductId());
        message.setSize(trade.getSize().toPlainString());
        message.setPrice(trade.getPrice().toPlainString());
        message.setSide(trade.getSide().name().toLowerCase());
        return message;
    }

    private CandleMessage candleMessage(Candle candle) {
        CandleMessage message = new CandleMessage();
        message.setProductId(candle.getProductId());
        message.setGranularity(candle.getGranularity());
        message.setTime(candle.getTime());
        message.setOpen(candle.getOpen().stripTrailingZeros().toPlainString());
        message.setClose(candle.getClose().stripTrailingZeros().toPlainString());
        message.setHigh(candle.getHigh().stripTrailingZeros().toPlainString());
        message.setLow(candle.getLow().stripTrailingZeros().toPlainString());
        message.setVolume(candle.getVolume().stripTrailingZeros().toPlainString());
        return message;
    }

    private OrderMessage orderMessage(Order order) {
        OrderMessage message = new OrderMessage();
        message.setType("order");
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setId(order.getOrderId());
        message.setPrice(order.getPrice().toPlainString());
        message.setSize(order.getSize().toPlainString());
        message.setFunds(order.getFunds().toPlainString());
        message.setSide(order.getSide().name().toLowerCase());
        message.setOrderType(order.getType().name().toLowerCase());
        message.setCreatedAt(order.getCreatedAt().toInstant().toString());
        message.setFillFees(order.getFillFees() != null ? order.getFillFees().toPlainString() : "0");
        message.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().toPlainString() : "0");
        message.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().toPlainString() : "0");
        message.setStatus(order.getStatus().name().toLowerCase());
        return message;
    }

    private AccountMessage accountMessage(Account account) {
        AccountMessage message = new AccountMessage();
        message.setType("funds");
        message.setUserId(account.getUserId());
        message.setCurrencyCode(account.getCurrency());
        message.setAvailable(
            account.getAvailable() != null ? account.getAvailable().stripTrailingZeros().toPlainString() : "0");
        message.setHold(account.getHold() != null ? account.getHold().stripTrailingZeros().toPlainString() : "0");
        return message;
    }

}
