package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.marketdata.manager.TickerManager;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.Trade;
import com.gitbitex.matchingengine.message.TradeMessage;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
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
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TickerThread extends KafkaConsumerThread<String, TradeMessage> implements ConsumerRebalanceListener {
    private final AppProperties appProperties;
    private final TickerManager tickerManager;
    private final Map<String, Ticker> tickerByProductId = new HashMap<>();

    public TickerThread(KafkaConsumer<String, TradeMessage> consumer, TickerManager tickerManager,
                        AppProperties appProperties) {
        super(consumer, logger);
        this.tickerManager = tickerManager;
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

        records.forEach(x -> {
            TradeMessage tradeMessage = x.value();
            refreshTicker(tradeMessage);
        });
        consumer.commitSync();
    }

    public void refreshTicker(Trade trade) {
        Ticker ticker = tickerByProductId.get(trade.getProductId());
        if (ticker == null) {
            ticker = tickerManager.getTicker(trade.getProductId());
        }
        if (ticker != null) {
            long diff = trade.getSequence() - ticker.getTradeId();
            if (diff <= 0) {
                return;
            } else if (diff > 1) {
                throw new RuntimeException("tradeId is discontinuous");
            }
        }

        if (ticker == null) {
            ticker = new Ticker();
            ticker.setProductId(trade.getProductId());
        }

        long time24h = DateUtil.round(ZonedDateTime.ofInstant(trade.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, 24 * 60).toEpochSecond();
        long time30d = DateUtil.round(ZonedDateTime.ofInstant(trade.getTime().toInstant(), ZoneId.systemDefault()),
                ChronoField.MINUTE_OF_DAY, 24 * 60 * 30).toEpochSecond();

        if (ticker.getTime24h() == null || ticker.getTime24h() != time24h) {
            ticker.setTime24h(time24h);
            ticker.setOpen24h(trade.getPrice());
            ticker.setClose24h(trade.getPrice());
            ticker.setHigh24h(trade.getPrice());
            ticker.setLow24h(trade.getPrice());
            ticker.setVolume24h(trade.getSize());
        } else {
            ticker.setClose24h(trade.getPrice());
            ticker.setHigh24h(ticker.getHigh24h().max(trade.getPrice()));
            ticker.setVolume24h(ticker.getVolume24h().add(trade.getSize()));
        }
        if (ticker.getTime30d() == null || ticker.getTime30d() != time30d) {
            ticker.setTime30d(time30d);
            ticker.setOpen30d(trade.getPrice());
            ticker.setClose30d(trade.getPrice());
            ticker.setHigh30d(trade.getPrice());
            ticker.setLow30d(trade.getPrice());
            ticker.setVolume30d(trade.getSize());
        } else {
            ticker.setClose30d(trade.getPrice());
            ticker.setHigh30d(ticker.getHigh30d().max(trade.getPrice()));
            ticker.setVolume30d(ticker.getVolume30d().add(trade.getSize()));
        }
        ticker.setLastSize(trade.getSize());
        ticker.setTime(trade.getTime());
        ticker.setPrice(trade.getPrice());
        ticker.setSide(trade.getSide());
        ticker.setTradeId(trade.getSequence());
        tickerByProductId.put(trade.getProductId(), ticker);

        tickerManager.saveTicker(ticker);
    }

}
