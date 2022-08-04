package com.gitbitex.marketdata;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * My job is to produce candles
 */
@Slf4j
public class CandleMakerThread extends KafkaConsumerThread<String, OrderBookLog> implements ConsumerRebalanceListener {
    private static final int[] MINUTES = new int[] {1, 5, 15, 30, 60, 360, 1440};
    private final List<String> productIds;
    private final Map<String, Map<Integer, Candle>> candlesByProductId = new HashMap<>();
    private final CandleRepository candleRepository;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public CandleMakerThread(List<String> productIds,
        CandleRepository candleRepository,
        KafkaConsumer<String, OrderBookLog> consumer,
        AppProperties appProperties) {
        super(consumer, logger);
        this.productIds = productIds;
        this.candleRepository = candleRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync();

        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            candlesByProductId.remove(productId);
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
            String productId = TopicUtil.parseProductIdFromTopic(partition.topic());
            candlesByProductId.put(productId, new HashMap<>());
        }
    }

    @Override
    protected void doSubscribe() {
        List<String> topics = productIds.stream()
            .map(x -> TopicUtil.getProductTopic(x, appProperties.getOrderBookLogTopic()))
            .collect(Collectors.toList());
        consumer.subscribe(topics, this);
    }

    @Override
    @SneakyThrows
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            if (log instanceof OrderMatchLog) {
                on(((OrderMatchLog)log), record.offset());
            }
        }

        if (uncommittedRecordCount > 1000) {
            consumer.commitSync();
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
        Map<Integer, Candle> candles = candlesByProductId.get(log.getProductId());
        Candle candle = candles.get(granularity);
        /*if (candle == null) {
            candle = candleRepository.findTopByProductIdAndGranularityOrderByTimeDesc(productId, granularity);
            if (candle != null) {
                candles.put(granularity, candle);
            }
        }*/

        if (candle != null) {
            if (candle.getTradeId() >= log.getTradeId()) {
                logger.info("discard {}", log.getSequence());
            } else if (candle.getTradeId() + 1 != log.getTradeId()) {
                throw new RuntimeException(String.format("unexpected tradeId: candle=%s, log=%s",
                    JSON.toJSONString(candle), JSON.toJSONString(log)));
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
