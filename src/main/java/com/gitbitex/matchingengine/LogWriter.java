package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.AccountChangeLog;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.OrderLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@RequiredArgsConstructor
public class LogWriter {
    private final KafkaMessageProducer producer;
    private final ExecutorService accountLogSender= Executors.newFixedThreadPool(1);
    private final ExecutorService orderLogSender= Executors.newFixedThreadPool(1);

    public void add(Log log) {
        if (true) return;
        //logger.info(JSON.toJSONString(log));
        if (log instanceof AccountChangeLog) {
            accountLogSender.execute(()->{
                producer.sendAccountLog((AccountChangeLog) log, null);
            });
        } else if (log instanceof OrderLog) {
            orderLogSender.execute(()->{
                producer.sendOrderBookLog((OrderLog) log, null);
            });
        }
    }
}
