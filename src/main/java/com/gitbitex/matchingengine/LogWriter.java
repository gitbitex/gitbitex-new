package com.gitbitex.matchingengine;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderType;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderMatchMessage;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogWriter {
    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic tradeTopic;
    private final RTopic orderBookTopic;

    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    //ThreadPoolExecutor accountLogSender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,accountQueue);
    ThreadPoolExecutor mainExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService kafkaExecutor = new StripedExecutorService(2);
    TreeMap<Long, AtomicLong> refCounterByCommandOffset = new TreeMap<>(Comparator.reverseOrder());

    public LogWriter(KafkaMessageProducer producer, RedissonClient redissonClient, AppProperties appProperties) {
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.appProperties = appProperties;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
        this.orderBookTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
    }

    public void flush(Long commandOffset, List<Object> dirtyObjects) {
        setRefCount(commandOffset, dirtyObjects.size());

        for (Object dirtyObject : dirtyObjects) {
            if (dirtyObject instanceof Order) {
                flush(commandOffset, ((Order)dirtyObject).clone());
            } else if (dirtyObject instanceof Account) {
                flush(commandOffset, ((Account)dirtyObject).clone());
            } else if (dirtyObject instanceof Trade) {
                flush(commandOffset, (Trade)dirtyObject);
            }
        }
    }

    public void flush(Long commandOffset, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(), data),
                (recordMetadata, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    decrRefCount(commandOffset);
                });
        });
    }

    public void flush(Long commandOffset, Order order) {
        kafkaExecutor.execute(order.getUserId(), () -> {
            String data = JSON.toJSONString(order);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), order.getUserId(), data),
                (recordMetadata, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    decrRefCount(commandOffset);
                });
        });
    }

    public void flush(Long commandOffset, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), trade.getProductId(), data),
                (recordMetadata, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    decrRefCount(commandOffset);
                });
        });
    }

    private void setRefCount(Long commandOffset, int count) {
        refCounterByCommandOffset.put(commandOffset, new AtomicLong(count));
    }

    private void decrRefCount(Long commandOffset) {
        if (refCounterByCommandOffset.get(commandOffset).decrementAndGet() == 0) {
            createSafePoint(commandOffset);
        }else{
            //logger.info("not safe");
        }
    }

    private void createSafePoint(Long commandOffset) {
        logger.info("creating safe point at command offset: {}", commandOffset);

    }

    public void onOrderReceived(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderReceivedMessage message = new OrderReceivedMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            message.setUserId(order.getUserId());
            message.setPrice(order.getPrice());
            message.setFunds(order.getRemainingFunds());
            message.setSide(order.getSide());
            message.setSize(order.getRemainingSize());
            message.setOrderId(order.getOrderId());
            message.setOrderType(order.getType());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderOpen(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderOpenMessage message = new OrderOpenMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            message.setRemainingSize(order.getRemainingSize());
            message.setPrice(order.getPrice());
            message.setSide(order.getSide());
            message.setOrderId(order.getOrderId());
            message.setUserId(order.getUserId());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderMatch(Order takerOrder, Order makerOrder, Trade trade, long sequence) {
        mainExecutor.execute(() -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data));
            tradeTopic.publishAsync(data);

            OrderMatchMessage message = new OrderMatchMessage();
            message.setSequence(sequence);
            message.setTradeId(trade.getTradeId());
            message.setProductId(trade.getProductId());
            message.setTakerOrderId(takerOrder.getOrderId());
            message.setMakerOrderId(makerOrder.getOrderId());
            message.setTakerUserId(takerOrder.getUserId());
            message.setMakerUserId(makerOrder.getUserId());
            message.setPrice(makerOrder.getPrice());
            message.setSize(trade.getSize());
            message.setFunds(trade.getFunds());
            message.setSide(makerOrder.getSide());
            message.setTime(takerOrder.getTime());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderDone(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderDoneMessage message = new OrderDoneMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            if (order.getType() != OrderType.MARKET) {
                message.setRemainingSize(order.getRemainingSize());
                message.setPrice(order.getPrice());
            }
            message.setRemainingFunds(order.getRemainingFunds());
            message.setRemainingSize(order.getRemainingSize());
            message.setSide(order.getSide());
            message.setOrderId(order.getOrderId());
            message.setUserId(order.getUserId());
            //log.setDoneReason(doneReason);
            message.setOrderType(order.getType());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

}
