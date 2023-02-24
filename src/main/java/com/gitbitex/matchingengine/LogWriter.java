package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LogWriter {
    private final KafkaMessageProducer messageProducer;

    public void add(Log o){

        logger.info(JSON.toJSONString(o));
        messageProducer.sendOrderBookLog(o,null);
    }
}
