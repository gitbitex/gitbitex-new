package com.gitbitex.marketdata;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.TickerManager;
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

@Slf4j
public class TickerThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final TickerManager tickerManager;
    private final AppProperties appProperties;
    private final RTopic tickerTopic;
    private Ticker ticker;

    public TickerThread(String productId, TickerManager tickerManager,
        RedissonClient redissonClient, KafkaConsumer<String, OrderBookLog> consumer,
        AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.appProperties = appProperties;
        this.tickerManager = tickerManager;
        this.tickerTopic = redissonClient.getTopic("ticker", StringCodec.INSTANCE);
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
                    ticker = tickerManager.getTicker(productId);

                    for (TopicPartition partition : partitions) {
                        if (ticker != null) {
                            consumer.seek(partition, ticker.getOrderBookLogOffset() + 1);
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

            if (ticker == null && log.getSequence() != 1) {
                throw new RuntimeException("unexpected sequence: sequence must be 1");
            } else if (log.getSequence() <= ticker.getSequence()) {
                logger.info("discard {}", log.getSequence());
                continue;
            } else if (log.getSequence() != ticker.getSequence() + 1) {
                throw new RuntimeException("unexpected sequence: sequence discontinuity");
            }

            if (log instanceof OrderMatchLog) {
                OrderMatchLog orderMatchLog = ((OrderMatchLog)log);
                orderMatchLog.setOffset(record.offset());
                logger.info(JSON.toJSONString(orderMatchLog));

                refreshTicker(orderMatchLog);
            }
        }
    }

    private void refreshTicker(OrderMatchLog log) {
        long time24h = DateUtil.round(
            ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, 24 * 60).toEpochSecond();
        long time30d = DateUtil.round(
            ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, 24 * 60 * 30).toEpochSecond();

        if (ticker == null) {
            ticker = new Ticker();
            ticker.setProductId(productId);
        }
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
        ticker.setTime(log.getTime().toInstant().toString());
        ticker.setPrice(log.getPrice());
        ticker.setSide(log.getSide().name().toLowerCase());
        ticker.setTradeId(log.getTradeId());
        ticker.setSequence(log.getSequence());

        tickerManager.saveTicker(ticker);
        tickerTopic.publish(JSON.toJSONString(ticker));

        /*TickerMessage tickerMessage = new TickerMessage();
        tickerMessage.setProductId(productId);
        tickerMessage.setTradeId(log.getTradeId());
        tickerMessage.setSequence(log.getSequence());
        tickerMessage.setTime(log.getTime().toInstant().toString());
        tickerMessage.setPrice(log.getPrice().toPlainString());
        tickerMessage.setSide(log.getSide().name());
        tickerMessage.setLastSize(log.getSize().toPlainString());
        tickerMessage.setTime24h(time24h);
        tickerMessage.setClose24h(close24h.toPlainString());
        tickerMessage.setOpen24h(open24h.toPlainString());
        tickerMessage.setHigh24h(high24h.toPlainString());
        tickerMessage.setLow24h(low24h.toPlainString());
        tickerMessage.setVolume24h(volume24h.toPlainString());
        tickerMessage.setTime30d(time30d);
        tickerMessage.setVolume30d(volume30d.toPlainString());
        tickerManager.saveTicker(tickerMessage.getProductId(), tickerMessage);
        marketMessagePublisher.publish(tickerMessage);*/
    }
}
