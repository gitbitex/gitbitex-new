package com.gitbitex.matchingengine;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderType;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class LogWriter {
    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic tradeTopic;
    private final RTopic orderBookTopic;
    BlockingQueue<Runnable> accountQueue = new LinkedBlockingQueue<>(1000000);
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    //ThreadPoolExecutor accountLogSender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,accountQueue);
    ThreadPoolExecutor mainExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService kafkaExecutor = new StripedExecutorService(100);

    public LogWriter(KafkaMessageProducer producer, RedissonClient redissonClient, AppProperties appProperties) {
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.appProperties = appProperties;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
        this.orderBookTopic = redissonClient.getTopic("orderBook", StringCodec.INSTANCE);
    }

    public void accountUpdated(Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(), data));
            accountTopic.publishAsync(data);
        });
    }

    public void orderUpdated(String productId, Order order) {
        kafkaExecutor.execute(order.getOrderId(), () -> {
            order.setProductId(productId);
            String data = JSON.toJSONString(order);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), productId, data));
            orderTopic.publishAsync(data);
        });
    }

    public void orderReceived(String productId, Order order, long sequence) {
        orderUpdated(productId, order);

        mainExecutor.execute(() -> {
            OrderReceivedLog log = new OrderReceivedLog();
            log.setSequence(sequence);
            log.setProductId(productId);
            log.setUserId(order.getUserId());
            log.setPrice(order.getPrice());
            log.setFunds(order.getRemainingFunds());
            log.setSide(order.getSide());
            log.setSize(order.getRemainingSize());
            log.setOrderId(order.getOrderId());
            log.setOrderType(order.getType());
            log.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(log));
        });
    }

    public void orderOpen(String productId, Order order, long sequence) {
        orderUpdated(productId, order);

        mainExecutor.execute(() -> {
            OrderOpenLog log = new OrderOpenLog();
            log.setSequence(sequence);
            log.setProductId(productId);
            log.setRemainingSize(order.getRemainingSize());
            log.setPrice(order.getPrice());
            log.setSide(order.getSide());
            log.setOrderId(order.getOrderId());
            log.setUserId(order.getUserId());
            log.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(log));
        });
    }

    public void orderMatch(String productId, Order takerOrder, Order makerOrder, Trade trade, long sequence) {
        orderUpdated(productId, takerOrder);
        orderUpdated(productId, makerOrder);

        mainExecutor.execute(() -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), productId, data));
            tradeTopic.publishAsync(data);

            OrderMatchLog log = new OrderMatchLog();
            log.setSequence(sequence);
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
            orderBookTopic.publishAsync(JSON.toJSONString(log));
        });

        //tickerBook.refreshTicker(productId, log);
    }

    public void orderDone(String productId, Order order, long sequence) {
        orderUpdated(productId, order);

        mainExecutor.execute(() -> {
            OrderDoneLog log = new OrderDoneLog();
            log.setSequence(sequence);
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
            //log.setDoneReason(doneReason);
            log.setOrderType(order.getType());
            log.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(log));
        });
    }

}
