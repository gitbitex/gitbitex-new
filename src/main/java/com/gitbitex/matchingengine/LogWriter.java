package com.gitbitex.matchingengine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.OrderLog;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.matchingengine.log.TickerMessage;
import com.gitbitex.matchingengine.log.TradeMessage;
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
    BlockingQueue<Runnable> accountQueue = new LinkedBlockingQueue<>(1000000);
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    //ThreadPoolExecutor accountLogSender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,accountQueue);
    ThreadPoolExecutor orderLogSender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService accountLogSender = new StripedExecutorService(100);

    public void add(Log log) {
        if (true)return;
        //logger.info(JSON.toJSONString(log));
        if (log instanceof AccountMessage) {
            sendAccountMessage((AccountMessage)log, null);
            publishAccountMessage((AccountMessage)log);
        } else if (log instanceof OrderMessage) {
            sendOrderMessage((OrderMessage)log, null);
            publishOrderMessage((OrderMessage)log);
        } else if (log instanceof TradeMessage) {
            sendTradeMessage((TradeMessage)log, null);
        } else if (log instanceof OrderLog) {
            publishOrderBookMessage((OrderLog)log);
        } else if (log instanceof TickerMessage) {
            publishTickerMessage((TickerMessage)log);
        }
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
