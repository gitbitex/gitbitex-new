package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.stripexecutor.StripedExecutorService;
import com.gitbitex.stripexecutor.StripedRunnable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
public class LogWriter {
    private final KafkaMessageProducer producer;

    BlockingQueue<Runnable> accountQueue = new LinkedBlockingQueue<>(1000000);
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    //ThreadPoolExecutor accountLogSender=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,accountQueue);
    ThreadPoolExecutor orderLogSender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);

    StripedExecutorService accountLogSender = new StripedExecutorService(100);

    private final RedissonClient redissonClient;

    public void add(Log log) {
        ;
        //if (true) return;
        //logger.info(JSON.toJSONString(log));
        if (log instanceof AccountChangeLog) {


        } else if (log instanceof OrderLog) {


            orderLogSender.execute(() -> {

                redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(log));
            });


            if (log instanceof OrderMatchLog) {
                Trade trade = trade((OrderMatchLog) log);
                //producer.sendTrade(trade, null);
            }
        }

        //System.out.println("accountQueue "+ accountQueue.size());
        //System.out.println("orderQueue "+ orderQueue.size());
    }

    private Trade trade(OrderMatchLog log) {
        Trade trade = new Trade();
        trade.setTradeId(log.getTradeId());
        trade.setTime(log.getTime());
        trade.setSize(log.getSize());
        trade.setPrice(log.getPrice());
        trade.setProductId(log.getProductId());
        trade.setMakerOrderId(log.getMakerOrderId());
        trade.setTakerOrderId(log.getTakerOrderId());
        trade.setSide(log.getSide());
        trade.setSequence(log.getSequence());
        trade.setOrderBookLogOffset(log.getOffset());
        return trade;
    }

    Order order(OrderReceivedLog log) {
        return null;
    }
}
