package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Ticker;
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
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class TickerThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final TickerManager tickerManager;
    private final AppProperties appProperties;

    private Ticker ticker;
    private long uncommittedRecordCount;

    public TickerThread(String productId, TickerManager tickerManager,
                        RedissonClient redissonClient, KafkaConsumer<String, OrderBookLog> consumer,
                        AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.appProperties = appProperties;
        this.tickerManager = tickerManager;

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
                OrderMatchLog orderMatchLog = ((OrderMatchLog) log);
                orderMatchLog.setOffset(record.offset());
                refreshTicker(orderMatchLog);
            }
        }

        if (uncommittedRecordCount > 1000) {
            consumer.commitAsync();
            uncommittedRecordCount = 0;
        }
    }

    private void refreshTicker(OrderMatchLog log) {
        if (ticker == null) {
            ticker = tickerManager.getTicker(productId);
            if (ticker == null) {
                ticker = new Ticker();
                ticker.setProductId(productId);
            }
        }

        if (ticker.getTradeId() >= log.getTradeId()) {
            logger.warn("discard duplicate match log");
            return;
        } else if (ticker.getTradeId() + 1 != log.getTradeId()) {
            throw new RuntimeException("bad match log: tradeId discontinuity");
        }


        long time24h = DateUtil.round(
                ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, 24 * 60).toEpochSecond();
        long time30d = DateUtil.round(
                ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, 24 * 60 * 30).toEpochSecond();

        if (ticker.getTime24h() == null || ticker.getTime24h() != time24h) {
            ticker.setTime24h(time24h);
            ticker.setOpen24h(log.getPrice());
            ticker.setClose24h(log.getPrice());
            ticker.setHigh24h(log.getPrice());
            ticker.setLow24h(log.getPrice());
            ticker.setVolume24h(log.getSize());
        } else {
            ticker.setClose24h(log.getPrice());
            ticker.setHigh24h(ticker.getHigh24h().max(log.getPrice()));
            ticker.setVolume24h(ticker.getVolume24h().add(log.getSize()));
        }
        if (ticker.getTime30d() == null || ticker.getTime30d() != time30d) {
            ticker.setTime30d(time30d);
            ticker.setOpen30d(log.getPrice());
            ticker.setClose30d(log.getPrice());
            ticker.setHigh30d(log.getPrice());
            ticker.setLow30d(log.getPrice());
            ticker.setVolume30d(log.getSize());
        } else {
            ticker.setClose30d(log.getPrice());
            ticker.setHigh30d(ticker.getHigh30d().max(log.getPrice()));
            ticker.setVolume30d(ticker.getVolume30d().add(log.getSize()));
        }
        ticker.setLastSize(log.getSize());
        ticker.setTime(log.getTime());
        ticker.setPrice(log.getPrice());
        ticker.setSide(log.getSide());
        ticker.setTradeId(log.getTradeId());
        ticker.setSequence(log.getSequence());
        tickerManager.saveTicker(ticker);
    }
}
