package com.gitbitex.marketdata;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.api.RedissonClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, OrderBookLog> {
    private static final int[] MINUTES = new int[]{1, 5, 15, 30, 60, 360, 1440};
    private final Map<Integer, Candle> candles = new ConcurrentHashMap<>();
    private final String productId;
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public CandleMakerThread(String productId, CandleRepository candleRepository, RedissonClient redissonClient,
                             KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.candleRepository = candleRepository;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                consumer.commitSync();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {

            }
        });
    }

    @Override
    @SneakyThrows
    protected void processRecords(ConsumerRecords<String, OrderBookLog> records) {
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            if (log instanceof OrderMatchLog) {
                on(((OrderMatchLog) log), record.offset());
            }
        }

        if (uncommittedRecordCount > 1000) {
            consumer.commitAsync();
            uncommittedRecordCount = 0;
        }
    }

    private void on(OrderMatchLog log, long offset) {
        List<Candle> candles = Arrays.stream(MINUTES)
                .parallel()
                .mapToObj(x -> makeCandle(log, offset, x))
                .collect(Collectors.toList());
        candleRepository.saveAll(candles);
    }

    private Candle makeCandle(OrderMatchLog log, long offset, int granularity) {
        Candle candle = candles.get(granularity);
        if (candle == null) {
            candle = candleRepository.findTopByProductIdAndGranularityOrderByTimeDesc(productId, granularity);
            if (candle != null) {
                candles.put(granularity, candle);
            }
        }

        if (candle != null) {
            if (candle.getTradeId() >= log.getTradeId()) {
                logger.info("discard {}", log.getSequence());
            } else if (candle.getTradeId() + 1 != log.getTradeId()) {
                throw new RuntimeException(String.format("unexpected tradeId: candle=%s, log=%s", JSON.toJSONString(candle), JSON.toJSONString(log)));
            }
        }

        long candleTime = DateUtil.round(
                ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();

        if (candle == null || candle.getTime() != candleTime) {
            candle = new Candle();
            candle.setProductId(log.getProductId());
            candle.setOpen(log.getPrice());
            candle.setClose(log.getPrice());
            candle.setLow(log.getPrice());
            candle.setHigh(log.getPrice());
            candle.setVolume(log.getSize());
            candle.setGranularity(granularity);
            candle.setTime(candleTime);
            candles.put(granularity, candle);
        } else {
            candle.setClose(log.getPrice());
            candle.setLow(candle.getLow().min(log.getPrice()));
            candle.setHigh(candle.getLow().max(log.getPrice()));
            candle.setVolume(candle.getVolume().add(log.getSize()));
        }
        candle.setOrderBookLogOffset(offset);
        candle.setTradeId(log.getTradeId());
        return candle;
    }
}
