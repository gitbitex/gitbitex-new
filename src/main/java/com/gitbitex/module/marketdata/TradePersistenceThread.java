package com.gitbitex.module.marketdata;

import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.module.marketdata.entity.Trade;
import com.gitbitex.kafka.KafkaConsumerThread;
import com.gitbitex.module.matchingengine.log.OrderBookLog;
import com.gitbitex.module.matchingengine.log.OrderMatchLog;
import com.gitbitex.module.marketdata.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;

    public TradePersistenceThread(String productId, TradeRepository tradeRepository,
        KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookLog> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()));
    }

    @Override
    protected void processRecords(KafkaConsumer<String, OrderBookLog> consumer,
        ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            if (log instanceof OrderMatchLog) {
                OrderMatchLog orderMatchLog = ((OrderMatchLog)log);
                orderMatchLog.setOffset(record.offset());
                logger.info(JSON.toJSONString(orderMatchLog));

                Trade trade = tradeRepository.findByProductIdAndTradeId(orderMatchLog.getProductId(),
                    orderMatchLog.getTradeId());
                if (trade != null) {
                    continue;
                }
                trade = new Trade();
                trade.setTradeId(orderMatchLog.getTradeId());
                trade.setTime(orderMatchLog.getTime());
                trade.setSize(orderMatchLog.getSize());
                trade.setPrice(orderMatchLog.getPrice());
                trade.setProductId(orderMatchLog.getProductId());
                trade.setMakerOrderId(orderMatchLog.getMakerOrderId());
                trade.setTakerOrderId(orderMatchLog.getTakerOrderId());
                trade.setSide(orderMatchLog.getSide());
                tradeRepository.save(trade);
            }
        }
        consumer.commitSync();
    }
}
