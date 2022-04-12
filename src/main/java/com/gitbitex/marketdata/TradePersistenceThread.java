package com.gitbitex.marketdata;

import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;
    private final RTopic tradeTopic;

    public TradePersistenceThread(String productId, TradeRepository tradeRepository,
        RedissonClient redissonClient,
        KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, OrderBookLog> consumer) {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()),
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    Trade trade = tradeRepository.findFirstByProductIdOrderByTimeDesc(productId);

                    for (TopicPartition partition : partitions) {
                        if (trade != null) {
                            consumer.seek(partition, trade.getOrderBookLogOffset() + 1);
                        }
                    }
                }
            });
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
                trade.setSequence(log.getSequence());
                trade.setOrderBookLogOffset(record.offset());
                tradeRepository.save(trade);
            }
        }
    }
}
