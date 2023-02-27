package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
@RequiredArgsConstructor
public class LogWriter {
    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final AtomicLong logSequence = new AtomicLong();
    BlockingQueue<Runnable> accountQueue = new LinkedBlockingQueue<>(1000000);
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    //ThreadPoolExecutor accountLogSender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,accountQueue);
    ThreadPoolExecutor orderLogSender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService accountLogSender = new StripedExecutorService(100);


    public void orderRejected(String productId, Order order, OrderRejectedLog.RejectReason rejectReason) {
        orderLogSender.execute(() -> {
            OrderMessage orderMessage = orderMessage(order, productId);
            orderMessage.setStatus(OrderStatus.REJECTED);
            add(orderMessage);

            OrderRejectedLog log = new OrderRejectedLog();
            log.setSequence(logSequence.incrementAndGet());
            log.setProductId(productId);
            log.setUserId(order.getUserId());
            log.setPrice(order.getPrice());
            log.setFunds(order.getRemainingFunds());
            log.setSide(order.getSide());
            log.setSize(order.getRemainingSize());
            log.setOrderId(order.getOrderId());
            log.setOrderType(order.getType());
            log.setTime(new Date());
            log.setRejectReason(rejectReason);
            add(log);
        });
    }


    public void orderReceived(String productId, Order order) {
        orderLogSender.execute(() -> {
            OrderMessage orderMessage = orderMessage(order, productId);
            orderMessage.setStatus(OrderStatus.RECEIVED);
            add(orderMessage);

            OrderReceivedLog log = new OrderReceivedLog();
            log.setSequence(logSequence.incrementAndGet());
            log.setProductId(productId);
            log.setUserId(order.getUserId());
            log.setPrice(order.getPrice());
            log.setFunds(order.getRemainingFunds());
            log.setSide(order.getSide());
            log.setSize(order.getRemainingSize());
            log.setOrderId(order.getOrderId());
            log.setOrderType(order.getType());
            log.setTime(new Date());
            add(log);
        });
    }

    public void orderOpen(String productId, Order order) {
        orderLogSender.execute(() -> {
            OrderMessage orderMessage = orderMessage(order, productId);
            orderMessage.setStatus(OrderStatus.OPEN);
            add(orderMessage);

            OrderOpenLog log = new OrderOpenLog();
            log.setSequence(logSequence.incrementAndGet());
            log.setProductId(productId);
            log.setRemainingSize(order.getRemainingSize());
            log.setPrice(order.getPrice());
            log.setSide(order.getSide());
            log.setOrderId(order.getOrderId());
            log.setUserId(order.getUserId());
            log.setTime(new Date());
            add(log);
        });
    }

    public void orderMatch(String productId, Order takerOrder, Order makerOrder, Trade trade) {
        orderLogSender.execute(() -> {
            TradeMessage tradeMessage = new TradeMessage();
            tradeMessage.setProductId(productId);
            tradeMessage.setTradeId(trade.getTradeId());
            tradeMessage.setTakerOrderId(takerOrder.getOrderId());
            tradeMessage.setMakerOrderId(takerOrder.getOrderId());
            tradeMessage.setPrice(makerOrder.getPrice());
            tradeMessage.setSize(trade.getSize());
            add(tradeMessage);

            OrderMatchLog log = new OrderMatchLog();
            log.setSequence(logSequence.incrementAndGet());
            log.setTradeId(trade.getTradeId());
            log.setProductId(productId);
            log.setTakerOrderId(takerOrder.getOrderId());
            log.setMakerOrderId(makerOrder.getOrderId());
            log.setTakerUserId(takerOrder.getUserId());
            log.setMakerUserId(makerOrder.getUserId());
            log.setPrice(makerOrder.getPrice());
            log.setSize(trade.getSize());
            log.setFunds(trade.getFunds());
            log.setSide(makerOrder.getSide());
            log.setTime(takerOrder.getTime());
            add(log);


        });

        //tickerBook.refreshTicker(productId, log);
    }

    public void orderDone(String productId, Order order) {
        orderLogSender.execute(() -> {
            OrderDoneLog.DoneReason doneReason = order.getRemainingSize().compareTo(BigDecimal.ZERO) > 0
                    ? OrderDoneLog.DoneReason.CANCELLED : OrderDoneLog.DoneReason.FILLED;

            OrderMessage orderMessage = orderMessage(order, productId);
            orderMessage.setStatus(
                    order.getRemainingSize().compareTo(BigDecimal.ZERO) > 0 ? OrderStatus.CANCELLED : OrderStatus.FILLED);
            add(orderMessage);

            OrderDoneLog log = new OrderDoneLog();
            log.setSequence(logSequence.incrementAndGet());
            log.setProductId(productId);
            if (order.getType() != OrderType.MARKET) {
                log.setRemainingSize(order.getRemainingSize());
                log.setPrice(order.getPrice());
            }
            log.setRemainingFunds(order.getRemainingFunds());
            log.setRemainingSize(order.getRemainingSize());
            log.setSide(order.getSide());
            log.setOrderId(order.getOrderId());
            log.setUserId(order.getUserId());
            log.setDoneReason(doneReason);
            log.setOrderType(order.getType());
            log.setTime(new Date());
            add(log);
        });
    }

    public void add(Log log) {
        //if (true) return;
        //logger.info(JSON.toJSONString(log));
        if (log instanceof AccountMessage) {
            sendAccountMessage((AccountMessage) log, null);
            publishAccountMessage((AccountMessage) log);
        } else if (log instanceof OrderMessage) {
            sendOrderMessage((OrderMessage) log, null);
            publishOrderMessage((OrderMessage) log);
        } else if (log instanceof TradeMessage) {
            sendTradeMessage((TradeMessage) log, null);
        } else if (log instanceof OrderLog) {
            publishOrderBookMessage((OrderLog) log);
        } else if (log instanceof TickerMessage) {
            publishTickerMessage((TickerMessage) log);
        }
    }

    private OrderMessage orderMessage(Order order, String productId) {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setProductId(productId);
        orderMessage.setUserId(order.getUserId());
        orderMessage.setPrice(order.getPrice());
        orderMessage.setFunds(order.getFunds());
        orderMessage.setSide(order.getSide());
        orderMessage.setSize(order.getSize());
        orderMessage.setOrderId(order.getOrderId());
        orderMessage.setOrderType(order.getType());
        orderMessage.setTime(order.getTime());
        orderMessage.setRemainingSize(order.getRemainingSize());
        orderMessage.setRemainingFunds(order.getRemainingFunds());
        return orderMessage;
    }

    private void publishTickerMessage(TickerMessage message) {
        String value = JSON.toJSONString(message);
        redissonClient.getBucket(message.getProductId() + ".ticker", StringCodec.INSTANCE).set(value);
        redissonClient.getTopic("ticker", StringCodec.INSTANCE).publishAsync(value);
    }

    private void publishAccountMessage(AccountMessage message) {
        redissonClient.getTopic("account", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(message));
    }

    public void publishOrderMessage(OrderMessage message) {
        redissonClient.getTopic("order", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(message));
    }

    public void publishOrderBookMessage(OrderLog message) {
        redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(message));
    }

    public Future<RecordMetadata> sendAccountMessage(AccountMessage log, Callback callback) {
        ProducerRecord<String, String> record = new ProducerRecord<>(appProperties.getAccountMessageTopic(),
                log.getUserId(), JSON.toJSONString(log));
        return producer.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendTradeMessage(TradeMessage log, Callback callback) {
        ProducerRecord<String, String> record = new ProducerRecord<>(appProperties.getTradeMessageTopic(),
                log.getProductId(), JSON.toJSONString(log));
        return producer.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendOrderMessage(OrderMessage log, Callback callback) {
        ProducerRecord<String, String> record = new ProducerRecord<>(appProperties.getOrderMessageTopic(),
                log.getProductId(), JSON.toJSONString(log));
        return producer.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

}
