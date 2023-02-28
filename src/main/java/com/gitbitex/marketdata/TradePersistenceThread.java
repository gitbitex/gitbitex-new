package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.log.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, TradeMessage>
    implements ConsumerRebalanceListener {
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;

    public TradePersistenceThread(TradeRepository tradeRepository,
        KafkaConsumer<String, TradeMessage> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
        }
        consumer.commitSync();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(appProperties.getTradeMessageTopic()), this);
    }

    @Override
    protected void doPoll() {
        ConsumerRecords<String, TradeMessage> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, Trade> trades = new HashMap<>();
        records.forEach(x -> {
            Trade trade = order(x.value());
            trades.put(trade.getId(), trade);
        });

        long t1 = System.currentTimeMillis();
        tradeRepository.saveAll(trades.values());
        long t2 = System.currentTimeMillis();
        logger.info("trades size: {} time: {}", trades.size(), t2 - t1);
    }

    private Trade order(TradeMessage log) {
        Trade trade = new Trade();
        trade.setId(log.getProductId() + "-" + log.getTradeId());
        trade.setTradeId(log.getTradeId());
        trade.setTime(log.getTime());
        trade.setSize(log.getSize());
        trade.setPrice(log.getPrice());
        trade.setProductId(log.getProductId());
        trade.setMakerOrderId(log.getMakerOrderId());
        trade.setTakerOrderId(log.getTakerOrderId());
        trade.setSide(log.getSide());
        return trade;
    }

}
