package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.TradeEntity;
import com.gitbitex.marketdata.manager.TradeManager;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, Message> implements ConsumerRebalanceListener {
    private final TradeManager tradeManager;
    private final AppProperties appProperties;

    public TradePersistenceThread(KafkaConsumer<String, Message> consumer, TradeManager tradeManager,
                                  AppProperties appProperties) {
        super(consumer, logger);
        this.tradeManager = tradeManager;
        this.appProperties = appProperties;
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
            if (message instanceof TradeMessage) {
                TradeEntity trade = trade((TradeMessage) message);
                trades.put(trade.getId(), trade);
            }
        });
        tradeManager.saveAll(trades.values());

        consumer.commitAsync();
    }

    private TradeEntity trade(TradeMessage message) {
        TradeEntity trade = new TradeEntity();
        trade.setId(message.getTrade().getProductId() + "-" + message.getTrade().getSequence());
        trade.setSequence(message.getTrade().getSequence());
        trade.setTime(message.getTrade().getTime());
        trade.setSize(message.getTrade().getSize());
        trade.setPrice(message.getTrade().getPrice());
        trade.setProductId(message.getTrade().getProductId());
        trade.setMakerOrderId(message.getTrade().getMakerOrderId());
        trade.setTakerOrderId(message.getTrade().getTakerOrderId());
        trade.setSide(message.getTrade().getSide());
        return trade;
    }
}
