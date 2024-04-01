package com.gitbitex.marketdata;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.TradeEntity;
import com.gitbitex.marketdata.manager.TradeManager;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    private final TradeManager tradeManager;
    private final AppProperties appProperties;
    private final RTopic tradeTopic;

    public TradePersistenceThread(KafkaConsumer<String, Message> consumer, TradeManager tradeManager,
                                  RedissonClient redissonClient,
                                  AppProperties appProperties) {
        super(consumer, logger);
        this.tradeManager = tradeManager;
        this.appProperties = appProperties;
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> collection) {

    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> collection) {

    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getMatchingEngineMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        Map<String, TradeEntity> trades = new HashMap<>();
        records.forEach(x -> {
            Message message = x.value();
            if (message instanceof TradeMessage tradeMessage) {
                TradeEntity tradeEntity = tradeEntity(tradeMessage);
                trades.put(tradeEntity.getId(), tradeEntity);
                tradeTopic.publishAsync(JSON.toJSONString(tradeMessage));
            }
        });
        tradeManager.saveAll(trades.values());

        consumer.commitAsync();
    }

    private TradeEntity tradeEntity(TradeMessage message) {
        Trade trade = message.getTrade();
        TradeEntity tradeEntity = new TradeEntity();
        tradeEntity.setId(trade.getProductId() + "-" + trade.getSequence());
        tradeEntity.setSequence(trade.getSequence());
        tradeEntity.setTime(trade.getTime());
        tradeEntity.setSize(trade.getSize());
        tradeEntity.setPrice(trade.getPrice());
        tradeEntity.setProductId(trade.getProductId());
        tradeEntity.setMakerOrderId(trade.getMakerOrderId());
        tradeEntity.setTakerOrderId(trade.getTakerOrderId());
        tradeEntity.setSide(trade.getSide());
        return tradeEntity;
    }
}
