package com.gitbitex.marketdata;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, OrderBookLog> {
    private static final int[] MINUTES = new int[] {1, 3, 5, 15, 30, 60, 120, 240, 360, 720, 1440, 43200};
    private final Map<Integer, Candle> candles = new HashMap<>();
    private final String productId;
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;
    private final RTopic candleTopic;

    public CandleMakerThread(String productId, CandleRepository candleRepository, RedissonClient redissonClient,
        KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.candleRepository = candleRepository;
        this.appProperties = appProperties;
        this.candleTopic = redissonClient.getTopic("candle", StringCodec.INSTANCE);
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
                    for (int minute : MINUTES) {
                        Candle candle = candleRepository.findTopByProductIdAndGranularityOrderByTimeDesc(productId,
                            minute);
                        if (candle != null) {
                            candles.put(minute, candle);
                        }
                    }

                    Candle minOffsetCandle = candles.values().stream().min(
                        Comparator.comparingLong(Candle::getOrderBookLogOffset)).orElse(null);

                    for (TopicPartition partition : partitions) {
                        if (minOffsetCandle != null) {
                            consumer.seek(partition, minOffsetCandle.getOrderBookLogOffset() + 1);
                        }
                    }
                }
            });
    }

    @Override
    @SneakyThrows
    protected void processRecords(KafkaConsumer<String, OrderBookLog> consumer,
        ConsumerRecords<String, OrderBookLog> records) {
        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();

            if (log instanceof OrderMatchLog) {
                OrderMatchLog orderMatchLog = ((OrderMatchLog)log);
                orderMatchLog.setOffset(record.offset());
                logger.info(JSON.toJSONString(orderMatchLog));

                for (int minute : MINUTES) {
                    makeCandle(orderMatchLog, record.offset(), minute);
                }
            }
        }
    }

    private void makeCandle(OrderMatchLog log, long offset, int granularity) {
        Candle candle = candles.get(granularity);

        if (candle != null) {
            if (candle.getTradeId() >= log.getTradeId()) {
                logger.info("discard {}", log.getSequence());
            } else if (candle.getTradeId() + 1 != log.getTradeId()) {
                throw new RuntimeException("unexpected tradeId");
            }
        }

        long candleTime = DateUtil.round(
            ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, granularity).toEpochSecond();

        if (candle == null || candle.getTime() != candleTime) {
            candle = new Candle();
            candle.setOpen(log.getPrice());
            candle.setClose(log.getPrice());
            candle.setLow(log.getPrice());
            candle.setHigh(log.getPrice());
            candle.setVolume(log.getSize());
            candle.setProductId(log.getProductId());
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

        candleRepository.save(candle);

        candleTopic.publish(JSON.toJSONString(candle));
    }
}
