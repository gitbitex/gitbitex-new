package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.TradeRepository;
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
public class TradePersistenceThread extends KafkaConsumerThread<String, TradeMessage>
        implements ConsumerRebalanceListener {
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;

    public TradePersistenceThread(KafkaConsumer<String, TradeMessage> consumer, TradeRepository tradeRepository,
                                  AppProperties appProperties) {
        super(consumer, logger);
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
        }
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
        var records = consumer.poll(Duration.ofSeconds(5));
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
        logger.info("saved {} trade(s) ({}ms)", trades.size(), System.currentTimeMillis() - t1);

        consumer.commitSync();
    }

    private Trade order(TradeMessage message) {
        Trade trade = new Trade();
        trade.setId(message.getProductId() + "-" + message.getSequence());
        trade.setSequence(message.getSequence());
        trade.setTime(message.getTime());
        trade.setSize(message.getSize());
        trade.setPrice(message.getPrice());
        trade.setProductId(message.getProductId());
        trade.setMakerOrderId(message.getMakerOrderId());
        trade.setTakerOrderId(message.getTakerOrderId());
        trade.setSide(message.getSide());
        return trade;
    }

}
