package com.gitbitex.marketdata;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.matchingengine.log.LogHandler;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.log.OrderRejectedLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class TickerThread extends KafkaConsumerThread<String, Log> implements ConsumerRebalanceListener, LogHandler {
    private final TickerManager tickerManager;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public TickerThread(TickerManager tickerManager, KafkaConsumer<String, Log> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.appProperties = appProperties;
        this.tickerManager = tickerManager;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        //consumer.commitSync();
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
        consumer.subscribe(Collections.singletonList(appProperties.getOrderBookLogTopic()), this);
    }

    @Override
    @SneakyThrows
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, Log> record : records) {
            Log log = record.value();
            log.setOffset(record.offset());
            LogDispatcher.dispatch(log, this);
        }

        if (uncommittedRecordCount > 10) {
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }

    @Override
    public void on(OrderRejectedLog log) {

    }

    @Override
    public void on(OrderReceivedLog log) {

    }

    @Override
    public void on(OrderOpenLog log) {

    }

    @Override
    public void on(OrderMatchLog log) {
        refreshTicker(log);
    }

    @Override
    public void on(OrderDoneLog log) {

    }

    @Override
    public void on(OrderFilledMessage log) {

    }

    @Override
    public void on(AccountChangeMessage log) {

    }

    private void refreshTicker(OrderMatchLog log) {
        Ticker ticker = tickerManager.getTicker(log.getProductId());
        if (ticker == null) {
            ticker = new Ticker();
            ticker.setProductId(log.getProductId());
        }

        if (ticker.getTradeId() >= log.getTradeId()) {
            logger.warn("discard duplicate match log");
            return;
        } else if (ticker.getTradeId() + 1 != log.getTradeId()) {
            throw new RuntimeException(
                String.format("tradeId discontinuity: expected=%s actual=%s %s %s", ticker.getTradeId() + 1,
                    log.getTradeId(), JSON.toJSONString(ticker), JSON.toJSONString(log)));
        }

        long time24h = DateUtil.round(ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, 24 * 60).toEpochSecond();
        long time30d = DateUtil.round(ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
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
