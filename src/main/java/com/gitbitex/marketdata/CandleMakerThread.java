package com.gitbitex.marketdata;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.log.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, TradeMessage> implements ConsumerRebalanceListener {
    private static final int[] MINUTES = new int[] {1, 5, 15, 30, 60, 360, 1440};
    private final Map<String, Map<Integer, Candle>> candlesByProductId = new HashMap<>();
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;
    long last = 0;

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
        //consumer.commitSync();
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
        ConsumerRecords<String, TradeMessage> records = consumer.poll(Duration.ofSeconds(3));
        if (records.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Candle> candles = new LinkedHashMap<>();
        records.forEach(x -> {
            //System.out.println(x.value().getTradeId());
            Trade trade = x.value();
            for (int minute : MINUTES) {
                long time = DateUtil.round(ZonedDateTime.ofInstant(trade.getTime().toInstant(), ZoneId.systemDefault()),
                    ChronoField.MINUTE_OF_DAY, minute).toEpochSecond();
                String candleId = trade.getProductId() + "-" + time + "-" + minute;
                Candle candle = candles.get(candleId);
                if (candle == null) {
                    candle = candleRepository.findById(candleId);
                }

                if (candle == null) {
                    candle = new Candle();
                    candle.setId(candleId);
                    candle.setProductId(trade.getProductId());
                    candle.setGranularity(minute);
                    candle.setTime(time);
                    candle.setProductId(trade.getProductId());
                    candle.setOpen(trade.getPrice());
                    candle.setClose(trade.getPrice());
                    candle.setLow(trade.getPrice());
                    candle.setHigh(trade.getPrice());
                    candle.setVolume(trade.getSize());
                    candle.setTradeId(trade.getTradeId());
                } else {
                    if (candle.getTradeId() >= trade.getTradeId()) {
                        //logger.warn("ignore trade: {}",trade.getTradeId());
                        break;
                    } else if (candle.getTradeId() + 1 != trade.getTradeId()) {
                        throw new RuntimeException("bad trade: " + " " + (candle.getTradeId()) + " " + trade.getTradeId());
                    }

                    candle.setClose(trade.getPrice());
                    candle.setLow(candle.getLow().min(trade.getPrice()));
                    candle.setHigh(candle.getLow().max(trade.getPrice()));
                    candle.setVolume(candle.getVolume().add(trade.getSize()));
                    candle.setTradeId(trade.getTradeId());
                }

                     candles.put(candle.getId(), candle);

            }
        });

        long t1 = System.currentTimeMillis();
        if (!candles.isEmpty()) {
            candleRepository.saveAll(candles.values());
        }
        long t2 = System.currentTimeMillis();
        //logger.info("candles size: {} time: {}", candles.size(), t2 - t1);

        consumer.commitSync();
    }

    private Candle makeCandle(Trade trade, int granularity) {
        long time = DateUtil.round(ZonedDateTime.ofInstant(trade.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();
        String candleId = trade.getProductId() + "-" + time + "-" + granularity;
        Candle candle = candleRepository.findById(candleId);
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
            candle.setTradeId(trade.getTradeId());
        } else {
            if (candle.getTradeId() >= trade.getTradeId()) {
                //logger.warn("ignore trade: {}",trade.getTradeId());
                return null;
            } else if (candle.getTradeId() + 1 != trade.getTradeId()) {
                throw new RuntimeException("bad trade: " + " " + (candle.getTradeId()) + " " + trade.getTradeId());
            }

            candle.setClose(trade.getPrice());
            candle.setLow(candle.getLow().min(trade.getPrice()));
            candle.setHigh(candle.getLow().max(trade.getPrice()));
            candle.setVolume(candle.getVolume().add(trade.getSize()));
            candle.setTradeId(trade.getTradeId());
        }
        return candle;
    }

}
