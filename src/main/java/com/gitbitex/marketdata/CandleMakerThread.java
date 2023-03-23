package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.message.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, TradeMessage> implements ConsumerRebalanceListener {
    private static final int[] GRANULARITY_ARR = new int[]{1, 5, 15, 30, 60, 360, 1440};
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;

    public CandleMakerThread(CandleRepository candleRepository, KafkaConsumer<String, TradeMessage> consumer,
                             AppProperties appProperties) {
        super(consumer, logger);
        this.candleRepository = candleRepository;
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
    @SneakyThrows
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Candle> candles = new LinkedHashMap<>();
        records.forEach(x -> {
            Trade trade = x.value();
            for (int granularity : GRANULARITY_ARR) {
                long time = DateUtil.round(ZonedDateTime.ofInstant(trade.getTime().toInstant(), ZoneId.systemDefault()),
                        ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();
                String candleId = trade.getProductId() + "-" + time + "-" + granularity;
                Candle candle = candles.get(candleId);
                if (candle == null) {
                    candle = candleRepository.findById(candleId);
                }

                if (candle == null) {
                    candle = new Candle();
                    candle.setId(candleId);
                    candle.setProductId(trade.getProductId());
                    candle.setGranularity(granularity);
                    candle.setTime(time);
                    candle.setProductId(trade.getProductId());
                    candle.setOpen(trade.getPrice());
                    candle.setClose(trade.getPrice());
                    candle.setLow(trade.getPrice());
                    candle.setHigh(trade.getPrice());
                    candle.setVolume(trade.getSize());
                    candle.setTradeId(trade.getSequence());
                } else {
                    if (candle.getTradeId() >= trade.getSequence()) {
                        //logger.warn("ignore trade: {}",trade.getTradeId());
                        continue;
                    } else if (candle.getTradeId() + 1 != trade.getSequence()) {
                        throw new RuntimeException(
                                "out of order sequence: " + " " + (candle.getTradeId()) + " " + trade.getSequence());
                    }
                    candle.setClose(trade.getPrice());
                    candle.setLow(candle.getLow().min(trade.getPrice()));
                    candle.setHigh(candle.getLow().max(trade.getPrice()));
                    candle.setVolume(candle.getVolume().add(trade.getSize()));
                    candle.setTradeId(trade.getSequence());
                }

                candles.put(candle.getId(), candle);
            }
        });

        if (!candles.isEmpty()) {
            long t1 = System.currentTimeMillis();
            candleRepository.saveAll(candles.values());
            logger.info("saved {} candle(s) ({}ms)", candles.size(), System.currentTimeMillis() - t1);
        }

        consumer.commitSync();
    }

}
