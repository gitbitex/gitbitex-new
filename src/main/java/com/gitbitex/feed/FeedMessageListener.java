package com.gitbitex.feed;

import com.alibaba.fastjson.JSON;
import com.gitbitex.account.entity.Account;
import com.gitbitex.feed.message.*;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.order.entity.Order;
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
    private final OrderBookManager orderBookManager;

    @PostConstruct
    public void run() {
        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Order order = JSON.parseObject(msg, Order.class);
            String channel = order.getUserId() + "." + order.getProductId() + ".order";
            sessionManager.sendMessageToChannel(channel, (orderMessage(order)));
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Account account = JSON.parseObject(msg, Account.class);
            String channel = account.getUserId() + "." + account.getCurrency() + ".funds";
            sessionManager.sendMessageToChannel(channel, (accountMessage(account)));
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Ticker ticker = JSON.parseObject(msg, Ticker.class);
            String channel = ticker.getProductId() + ".ticker";
            sessionManager.sendMessageToChannel(channel, (new TickerMessage(ticker)));
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
            OrderBookLog log = JSON.parseObject(msg, OrderBookLog.class);
            String fullChannel = log.getProductId() + ".full";
            switch (log.getType()) {
                case RECEIVED:
                    OrderReceivedLog orderReceivedLog = JSON.parseObject(msg, OrderReceivedLog.class);
                    sessionManager.sendMessageToChannel(fullChannel,
                            (orderReceivedMessage(orderReceivedLog)));
                    break;
                case MATCH:
                    OrderMatchLog orderMatchLog = JSON.parseObject(msg, OrderMatchLog.class);
                    String matchChannel = log.getProductId() + ".match";
                    sessionManager.sendMessageToChannel(matchChannel, (matchMessage(orderMatchLog)));
                    sessionManager.sendMessageToChannel(fullChannel, (matchMessage(orderMatchLog)));
                    break;
                case OPEN:
                    OrderOpenLog orderOpenLog = JSON.parseObject(msg, OrderOpenLog.class);
                    sessionManager.sendMessageToChannel(fullChannel, (orderOpenMessage(orderOpenLog)));
                    break;
                case DONE:
                    OrderDoneLog orderDoneLog = JSON.parseObject(msg, OrderDoneLog.class);
                    sessionManager.sendMessageToChannel(fullChannel, (orderDoneMessage(orderDoneLog)));
                    break;
                default:
            }
        });
    }

    private OrderReceivedMessage orderReceivedMessage(OrderReceivedLog log) {
        OrderReceivedMessage message = new OrderReceivedMessage();
        message.setProductId(log.getProductId());
        message.setTime(log.getTime().toInstant().toString());
        message.setSequence(log.getSequence());
        message.setOrderId(log.getOrder().getOrderId());
        message.setSize(log.getOrder().getSize().stripTrailingZeros().toPlainString());
        message.setPrice(
                log.getOrder().getPrice() != null ? log.getOrder().getPrice().stripTrailingZeros().toPlainString() :
                        null);
        message.setFunds(
                log.getOrder().getFunds() != null ? log.getOrder().getFunds().stripTrailingZeros().toPlainString() :
                        null);
        message.setSide(log.getOrder().getSide().name().toUpperCase());
        message.setOrderType(log.getOrder().getType().name().toUpperCase());
        return message;
    }

    private OrderMatchMessage matchMessage(OrderMatchLog log) {
        OrderMatchMessage message = new OrderMatchMessage();
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

    private OrderOpenMessage orderOpenMessage(OrderOpenLog log) {
        OrderOpenMessage message = new OrderOpenMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setPrice(log.getPrice().stripTrailingZeros().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        message.setRemainingSize(log.getRemainingSize().toPlainString());
        return message;
    }

    private OrderDoneMessage orderDoneMessage(OrderDoneLog log) {
        OrderDoneMessage message = new OrderDoneMessage();
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
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setId(order.getOrderId());
        message.setPrice(order.getPrice().stripTrailingZeros().toPlainString());
        message.setSize(order.getSize().stripTrailingZeros().toPlainString());
        message.setFunds(order.getFunds().stripTrailingZeros().toPlainString());
        message.setSide(order.getSide().name().toLowerCase());
        message.setOrderType(order.getType().name().toLowerCase());
        message.setCreatedAt(order.getCreatedAt().toInstant().toString());
        message.setFillFees(
                order.getFillFees() != null ? order.getFillFees().stripTrailingZeros().toPlainString() : "0");
        message.setFilledSize(
                order.getFilledSize() != null ? order.getFilledSize().stripTrailingZeros().toPlainString() : "0");
        message.setExecutedValue(
                order.getExecutedValue() != null ? order.getExecutedValue().stripTrailingZeros().toPlainString() : "0");
        message.setStatus(order.getStatus().name().toLowerCase());
        return message;
    }

    private AccountMessage accountMessage(Account account) {
        AccountMessage message = new AccountMessage();
        message.setUserId(account.getUserId());
        message.setCurrencyCode(account.getCurrency());
        message.setAvailable(
                account.getAvailable() != null ? account.getAvailable().stripTrailingZeros().toPlainString() : "0");
        message.setHold(account.getHold() != null ? account.getHold().stripTrailingZeros().toPlainString() : "0");
        return message;
    }

}
