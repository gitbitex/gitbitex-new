package com.gitbitex.feed;

import com.alibaba.fastjson.JSON;
import com.gitbitex.feed.message.*;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.matchingengine.L2OrderBook;
import com.gitbitex.matchingengine.message.*;
import com.gitbitex.stripexecutor.StripedExecutorService;
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
    private final StripedExecutorService callbackExecutor =
            new StripedExecutorService(Runtime.getRuntime().availableProcessors());

    @PostConstruct
    public void run() {
        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            OrderMessage orderMessage = JSON.parseObject(msg, OrderMessage.class);
            callbackExecutor.execute(orderMessage.getUserId(), () -> {
                String channel = orderMessage.getUserId() + "." + orderMessage.getProductId() + ".order";
                sessionManager.broadcast(channel, orderFeedMessage(orderMessage));
            });
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            AccountMessage accountMessage = JSON.parseObject(msg, AccountMessage.class);
            callbackExecutor.execute(accountMessage.getUserId(), () -> {
                String channel = accountMessage.getUserId() + "." + accountMessage.getCurrency() + ".funds";
                sessionManager.broadcast(channel, accountFeedMessage(accountMessage));
            });
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            TickerMessage tickerMessage = JSON.parseObject(msg, TickerMessage.class);
            callbackExecutor.execute(tickerMessage.getProductId(), () -> {
                String channel = tickerMessage.getProductId() + ".ticker";
                sessionManager.broadcast(channel, tickerFeedMessage(tickerMessage));
            });
        });

        redissonClient.getTopic("candle", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            callbackExecutor.execute(() -> {
                Candle candle = JSON.parseObject(msg, Candle.class);
                String channel = candle.getProductId() + ".candle_" + candle.getGranularity() * 60;
                sessionManager.broadcast(channel, (candleMessage(candle)));
            });
        });

        redissonClient.getTopic("l2_batch", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            L2OrderBook l2OrderBook = JSON.parseObject(msg, L2OrderBook.class);
            callbackExecutor.execute(l2OrderBook.getProductId(), () -> {
                String channel = l2OrderBook.getProductId() + ".level2";
                sessionManager.broadcast(channel, l2OrderBook);
            });
        });

        redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            OrderBookMessage message = JSON.parseObject(msg, OrderBookMessage.class);
            callbackExecutor.execute(message.getProductId(), () -> {
                switch (message.getType()) {
                    case ORDER_RECEIVED:
                        OrderReceivedMessage orderReceivedMessage = JSON.parseObject(msg, OrderReceivedMessage.class);
                        sessionManager.broadcast(orderReceivedMessage.getProductId() + ".full",
                                (orderReceivedMessage(orderReceivedMessage)));
                        break;
                    case ORDER_MATCH:
                        OrderMatchMessage orderMatchMessage = JSON.parseObject(msg, OrderMatchMessage.class);
                        String matchChannel = orderMatchMessage.getProductId() + ".match";
                        sessionManager.broadcast(matchChannel, (matchMessage(orderMatchMessage)));
                        sessionManager.broadcast(orderMatchMessage.getProductId() + ".full",
                                (matchMessage(orderMatchMessage)));
                        break;
                    case ORDER_OPEN:
                        OrderOpenMessage orderOpenMessage = JSON.parseObject(msg, OrderOpenMessage.class);
                        sessionManager.broadcast(orderOpenMessage.getProductId() + ".full",
                                (orderOpenMessage(orderOpenMessage)));
                        break;
                    case ORDER_DONE:
                        OrderDoneMessage orderDoneMessage = JSON.parseObject(msg, OrderDoneMessage.class);
                        sessionManager.broadcast(orderDoneMessage.getProductId() + ".full",
                                (orderDoneMessage(orderDoneMessage)));
                        break;
                    default:
                }
            });
        });
    }

    private OrderReceivedFeedMessage orderReceivedMessage(OrderReceivedMessage log) {
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

    private OrderMatchFeedMessage matchMessage(OrderMatchMessage log) {
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

    private OrderOpenFeedMessage orderOpenMessage(OrderOpenMessage log) {
        OrderOpenFeedMessage message = new OrderOpenFeedMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        message.setRemainingSize(log.getRemainingSize().toPlainString());
        return message;
    }

    private OrderDoneFeedMessage orderDoneMessage(OrderDoneMessage log) {
        OrderDoneFeedMessage message = new OrderDoneFeedMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        if (log.getPrice() != null) {
            message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        }
        message.setSide(log.getSide().name().toLowerCase());
        //message.setReason(log.getDoneReason().name().toUpperCase());
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
        message.setId(order.getId());
        message.setPrice(order.getPrice().stripTrailingZeros().toPlainString());
        message.setSize(order.getSize().stripTrailingZeros().toPlainString());
        message.setFunds(order.getFunds().stripTrailingZeros().toPlainString());
        message.setSide(order.getSide().name().toLowerCase());
        message.setOrderType(order.getType().name().toLowerCase());
        message.setCreatedAt(order.getTime().toInstant().toString());
        message.setFillFees(
                order.getFillFees() != null ? order.getFillFees().stripTrailingZeros().toPlainString() : "0");
        message.setFilledSize(order.getSize().subtract(order.getRemainingSize()).stripTrailingZeros().toPlainString());
        message.setExecutedValue(order.getFunds().subtract(order.getRemainingFunds()).stripTrailingZeros().toPlainString());
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
