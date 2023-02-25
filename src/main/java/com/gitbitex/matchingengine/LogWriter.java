package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LogWriter {
    private final KafkaMessageProducer producer;

    public void add(Log log){
        long t1=System.currentTimeMillis();
        //logger.info(JSON.toJSONString(log));
        //producer.sendOrderBookLog(log,null);
        long t2=System.currentTimeMillis();
        long diff=t2-t1;
    }
}
