package com.gitbitex.feed;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;

import com.gitbitex.feed.message.AccountFeedMessage;
import com.gitbitex.feed.message.CandleFeedMessage;
import com.gitbitex.feed.message.OrderDoneFeedMessage;
import com.gitbitex.feed.message.OrderFeedMessage;
import com.gitbitex.feed.message.OrderMatchFeedMessage;
import com.gitbitex.feed.message.OrderOpenFeedMessage;
import com.gitbitex.feed.message.OrderReceivedFeedMessage;
import com.gitbitex.feed.message.TickerFeedMessage;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.log.TickerMessage;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
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
    private final OrderBookManager orderBookManager;

    @PostConstruct
    public void run() {
        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            OrderMessage orderMessage = JSON.parseObject(msg, OrderMessage.class);
            String channel = orderMessage.getUserId() + "." + orderMessage.getProductId() + ".order";
            sessionManager.sendMessageToChannel(channel, orderFeedMessage(orderMessage));
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            AccountMessage accountMessage = JSON.parseObject(msg, AccountMessage.class);
            String channel = accountMessage.getUserId() + "." + accountMessage.getCurrency() + ".funds";
            sessionManager.sendMessageToChannel(channel, accountFeedMessage(accountMessage));
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            TickerMessage tickerMessage = JSON.parseObject(msg, TickerMessage.class);
            String channel = tickerMessage.getProductId() + ".ticker";
            sessionManager.sendMessageToChannel(channel, tickerFeedMessage(tickerMessage));
        });

        redissonClient.getTopic("candle", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Candle candle = JSON.parseObject(msg, Candle.class);
            String channel = candle.getProductId() + ".candle_" + candle.getGranularity() * 60;
            sessionManager.sendMessageToChannel(channel, (candleMessage(candle)));
        });

        redissonClient.getTopic("l2_batch", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            L2OrderBook l2OrderBook = orderBookManager.getL2BatchOrderBook(msg);
            String channel = l2OrderBook.getProductId() + ".level2";
            sessionManager.sendMessageToChannel(channel, l2OrderBook);
        });

        redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Log log = JSON.parseObject(msg, Log.class);
            switch (log.getType()) {
                case ORDER_RECEIVED:
                    OrderReceivedLog orderReceivedLog = JSON.parseObject(msg, OrderReceivedLog.class);
                    sessionManager.sendMessageToChannel(orderReceivedLog.getProductId() + ".full",
                        (orderReceivedMessage(orderReceivedLog)));
                    break;
                case ORDER_MATCH:
                    OrderMatchLog orderMatchLog = JSON.parseObject(msg, OrderMatchLog.class);
                    String matchChannel = orderMatchLog.getProductId() + ".match";
                    sessionManager.sendMessageToChannel(matchChannel, (matchMessage(orderMatchLog)));
                    sessionManager.sendMessageToChannel(orderMatchLog.getProductId() + ".full",
                        (matchMessage(orderMatchLog)));
                    break;
                case ORDER_OPEN:
                    OrderOpenLog orderOpenLog = JSON.parseObject(msg, OrderOpenLog.class);
                    sessionManager.sendMessageToChannel(orderOpenLog.getProductId() + ".full",
                        (orderOpenMessage(orderOpenLog)));
                    break;
                case ORDER_DONE:
                    OrderDoneLog orderDoneLog = JSON.parseObject(msg, OrderDoneLog.class);
                    sessionManager.sendMessageToChannel(orderDoneLog.getProductId() + ".full",
                        (orderDoneMessage(orderDoneLog)));
                    break;
                default:
            }
        });
    }

    private OrderReceivedFeedMessage orderReceivedMessage(OrderReceivedLog log) {
        OrderReceivedFeedMessage message = new OrderReceivedFeedMessage();
        message.setProductId(log.getProductId());
        message.setTime(log.getTime().toInstant().toString());
        message.setSequence(log.getSequence());
        message.setOrderId(log.getOrderId());
        message.setSize(log.getSize().stripTrailingZeros().toPlainString());
        message.setPrice(log.getPrice() != null ? log.getPrice().stripTrailingZeros().toPlainString() : null);
        message.setFunds(log.getFunds() != null ? log.getFunds().stripTrailingZeros().toPlainString() : null);
        message.setSide(log.getSide().name().toUpperCase());
        message.setOrderType(log.getType().name().toUpperCase());
        return message;
    }

    private OrderMatchFeedMessage matchMessage(OrderMatchLog log) {
        OrderMatchFeedMessage message = new OrderMatchFeedMessage();
        message.setTradeId(log.getTradeId());
        message.setSequence(log.getSequence());
        message.setTakerOrderId(log.getTakerOrderId());
        message.setMakerOrderId(log.getMakerOrderId());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setSize(log.getSize().stripTrailingZeros().toPlainString());
        message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        return message;
    }

    private OrderOpenFeedMessage orderOpenMessage(OrderOpenLog log) {
        OrderOpenFeedMessage message = new OrderOpenFeedMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        message.setRemainingSize(log.getRemainingSize().toPlainString());
        return message;
    }

    private OrderDoneFeedMessage orderDoneMessage(OrderDoneLog log) {
        OrderDoneFeedMessage message = new OrderDoneFeedMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        if (log.getPrice() != null) {
            message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        }
        message.setSide(log.getSide().name().toLowerCase());
        message.setReason(log.getDoneReason().name().toUpperCase());
        if (log.getRemainingSize() != null) {
            message.setRemainingSize(log.getRemainingSize().stripTrailingZeros().toPlainString());
        }
        return message;
    }

    private CandleFeedMessage candleMessage(Candle candle) {
        CandleFeedMessage message = new CandleFeedMessage();
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

    private OrderFeedMessage orderFeedMessage(OrderMessage order) {
        OrderFeedMessage message = new OrderFeedMessage();
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setId(order.getOrderId());
        message.setPrice(order.getPrice().stripTrailingZeros().toPlainString());
        message.setSize(order.getSize().stripTrailingZeros().toPlainString());
        message.setFunds(order.getFunds().stripTrailingZeros().toPlainString());
        message.setSide(order.getSide().name().toLowerCase());
        message.setOrderType(order.getType().name().toLowerCase());
        message.setCreatedAt(order.getTime().toInstant().toString());
        message.setFillFees(
            order.getFillFees() != null ? order.getFillFees().stripTrailingZeros().toPlainString() : "0");
        message.setFilledSize(
            order.getFilledSize() != null ? order.getFilledSize().stripTrailingZeros().toPlainString() : "0");
        message.setExecutedValue(
            order.getExecutedValue() != null ? order.getExecutedValue().stripTrailingZeros().toPlainString() : "0");
        message.setStatus(order.getStatus().name().toLowerCase());
        return message;
    }

    private AccountFeedMessage accountFeedMessage(AccountMessage message) {
        AccountFeedMessage accountFeedMessage = new AccountFeedMessage();
        accountFeedMessage.setUserId(message.getUserId());
        accountFeedMessage.setCurrencyCode(message.getCurrency());
        accountFeedMessage.setAvailable(message.getAvailable().stripTrailingZeros().toPlainString());
        accountFeedMessage.setHold(message.getHold().stripTrailingZeros().toPlainString());
        return accountFeedMessage;
    }

    private TickerFeedMessage tickerFeedMessage(TickerMessage ticker) {
        TickerFeedMessage tickerFeedMessage = new TickerFeedMessage();
        tickerFeedMessage.setProductId(ticker.getProductId());
        tickerFeedMessage.setTradeId(ticker.getTradeId());
        tickerFeedMessage.setSequence(ticker.getSequence());
        tickerFeedMessage.setTime(ticker.getTime().toInstant().toString());
        tickerFeedMessage.setPrice(ticker.getPrice().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setSide(ticker.getSide().name().toLowerCase());
        tickerFeedMessage.setLastSize(ticker.getLastSize().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setClose24h(ticker.getClose24h().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setOpen24h(ticker.getOpen24h().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setHigh24h(ticker.getHigh24h().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setLow24h(ticker.getLow24h().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setVolume24h(ticker.getVolume24h().stripTrailingZeros().toPlainString());
        tickerFeedMessage.setVolume30d(ticker.getVolume30d().stripTrailingZeros().toPlainString());
        return tickerFeedMessage;
    }

}
