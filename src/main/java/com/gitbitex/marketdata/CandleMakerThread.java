package com.gitbitex.marketdata;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.log.OrderMessage;
import com.gitbitex.matchingengine.log.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, TradeMessage> implements ConsumerRebalanceListener {
    private static final int[] MINUTES = new int[]{1, 5, 15, 30, 60, 360, 1440};
    private final Map<String, Map<Integer, Candle>> candlesByProductId = new HashMap<>();
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

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

    long tradeId = 0;

    @Override
    @SneakyThrows
    protected void doPoll() {
        ConsumerRecords<String, TradeMessage> records = consumer.poll(Duration.ofSeconds(5));
        if (records.isEmpty()) {
            return;
        }

        Map<String, TradeMessage> tradeMessageMap = new HashMap<>();

        Map<String, Candle> candleCache =new HashMap<>();
        List<Candle> candles = new ArrayList<>();
        records.forEach(x -> {
            for (int minute : MINUTES) {
                /*Candle candle = makeCandle(x.value(), minute,candleCache);
                candles.add(candle);*/
            }
        });

        long t1 = System.currentTimeMillis();
        candleRepository.saveAll(candles);
        long t2 = System.currentTimeMillis();
        logger.info("candles size: {} time: {}", candles.size(), t2 - t1);
    }

    private Candle makeCandle(Candle candle, List<TradeMessage> trades) {
        return null;
/*
        long time1 = DateUtil.round(ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();

        trades.stream().map(x->{
            long time = DateUtil.round(ZonedDateTime.ofInstant(x.getTime().toInstant(), ZoneId.systemDefault()),
                    ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();
            x.setTime(new Date(time));
            return x;
        })

        String candleId = productId + "-" + time + "-" + granularity;
        Candle candle = candleRepository.findById(candleId);
        if (candle == null) {
            candle = new Candle();
            candle.setId(candleId);
            candle.setProductId(log.getProductId());
        }

        for (TradeMessage log : trades) {

        }

        //if (candle != null) {
            if (candle.getTradeId() >= log.getTradeId()) {
                logger.info("discard {}", log.getTradeId());
            } else if (candle.getTradeId() + 1 != log.getTradeId()) {
                throw new RuntimeException(
                        String.format("unexpected tradeId: candle=%s, log=%s", candle.getTradeId(), log.getTradeId()));
            }
        //}

        if (candle == null) {
            candle = new Candle();
            candle.setId(candleId);
            candle.setProductId(log.getProductId());
            candle.setOpen(log.getPrice());
            candle.setClose(log.getPrice());
            candle.setLow(log.getPrice());
            candle.setHigh(log.getPrice());
            candle.setVolume(log.getSize());
            candle.setGranularity(granularity);
            candle.setTime(time);
        } else {
            candle.setClose(log.getPrice());
            candle.setLow(candle.getLow().min(log.getPrice()));
            candle.setHigh(candle.getLow().max(log.getPrice()));
            candle.setVolume(candle.getVolume().add(log.getSize()));
        }
        candle.setTradeId(log.getTradeId());
        return candle;*/
    }
}
