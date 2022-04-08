package com.gitbitex.marketdata;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.MarketMessagePublisher;
import com.gitbitex.matchingengine.TickerManager;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.marketmessage.CandleMessage;
import com.gitbitex.matchingengine.marketmessage.MarketMessage;
import com.gitbitex.matchingengine.marketmessage.MatchMessage;
import com.gitbitex.matchingengine.marketmessage.TickerMessage;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.redisson.client.codec.StringCodec;

@Slf4j
public class MarketDataMakerThread extends KafkaConsumerThread<String, OrderBookLog> {
    private static final int[] MINUTES = new int[] {1, 3, 5, 15, 30, 60, 120, 240, 360, 720, 1440, 43200};
    private final String productId;
    private final CandleRepository candleRepository;
    private final Map<Integer, Candle> candles = new HashMap<>();
    private final BlockingQueue<Candle> candleQueue = new LinkedBlockingQueue<>(100000);
    private final BlockingQueue<MarketMessage> feedMessageQueue = new LinkedBlockingQueue<>(100000);
    private final CandlePersistenceThread candlePersistenceThread;
    private final FeedMessagePublishThread feedMessagePublishThread;
    private final AppProperties appProperties;

    public MarketDataMakerThread(String productId, CandleRepository candleRepository, TickerManager tickerManager,
        MarketMessagePublisher marketMessagePublisher, KafkaConsumer<String, OrderBookLog> consumer,
        AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.candleRepository = candleRepository;
        this.candlePersistenceThread = new CandlePersistenceThread(candleQueue, candleRepository);
        this.candlePersistenceThread.setName("CandlePersistence-" + productId + "-" + candlePersistenceThread.getId());
        this.feedMessagePublishThread = new FeedMessagePublishThread(marketMessagePublisher, tickerManager,
            feedMessageQueue);
        this.appProperties = appProperties;
    }

    @Override
    public void start() {
        super.start();
        this.candlePersistenceThread.start();
        this.feedMessagePublishThread.start();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.candlePersistenceThread.interrupt();
        this.feedMessagePublishThread.interrupt();
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

                makeTickerMessage(orderMatchLog);

                makeMatchMessage(orderMatchLog);
            }
        }
    }

    private void makeCandle(OrderMatchLog log, long offset, int granularity) throws InterruptedException {
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
        candleQueue.put(candle);

        CandleMessage candleMessage = new CandleMessage();
        candleMessage.setProductId(productId);
        candleMessage.setGranularity(granularity);
        candleMessage.setTime(candle.getTime());
        candleMessage.setOpen(candle.getOpen().stripTrailingZeros().toPlainString());
        candleMessage.setClose(candle.getClose().stripTrailingZeros().toPlainString());
        candleMessage.setHigh(candle.getHigh().stripTrailingZeros().toPlainString());
        candleMessage.setLow(candle.getLow().stripTrailingZeros().toPlainString());
        candleMessage.setVolume(candle.getVolume().stripTrailingZeros().toPlainString());
        if (!feedMessageQueue.offer(candleMessage)) {
            logger.warn("feedMessageQueue is full");
        }
    }

    private void makeTickerMessage(OrderMatchLog log) {
        TickerMessage tickerMessage = new TickerMessage();
        tickerMessage.setProductId(productId);
        tickerMessage.setTradeId(log.getTradeId());
        tickerMessage.setSequence(log.getSequence());
        tickerMessage.setTime(log.getTime().toInstant().toString());
        tickerMessage.setPrice(log.getPrice().toPlainString());
        tickerMessage.setSide(log.getSide().name());
        tickerMessage.setLastSize(log.getSize().toPlainString());

        Candle candle24h = candles.get(24 * 60);
        if (candle24h != null) {
            tickerMessage.setClose24h(candle24h.getClose().toPlainString());
            tickerMessage.setOpen24h(candle24h.getOpen().toPlainString());
            tickerMessage.setHigh24h(candle24h.getHigh().toPlainString());
            tickerMessage.setLow24h(candle24h.getLow().toPlainString());
            tickerMessage.setVolume24h(candle24h.getVolume().toPlainString());
        }

        Candle candle30d = candles.get(24 * 60 * 30);
        if (candle30d != null) {
            tickerMessage.setVolume30d(candle30d.getVolume().toPlainString());
        }

        // if the queue is full, discard the ticker message directly, and a new ticker will be generated at the next
        // trade
        if (!feedMessageQueue.offer(tickerMessage)) {
            logger.warn("feedMessageQueue is full");
        }
    }

    private void makeMatchMessage(OrderMatchLog log) {
        MatchMessage matchMessage = new MatchMessage();
        matchMessage.setTradeId(log.getTradeId());
        matchMessage.setSequence(log.getSequence());
        matchMessage.setTakerOrderId(log.getTakerOrderId());
        matchMessage.setMakerOrderId(log.getMakerOrderId());
        matchMessage.setTime(log.getTime().toInstant().toString());
        matchMessage.setProductId(log.getProductId());
        matchMessage.setSize(log.getSize().toPlainString());
        matchMessage.setPrice(log.getPrice().toPlainString());
        matchMessage.setSide(log.getSide().name().toLowerCase());

        // discard the match message if the queue is full
        if (!feedMessageQueue.offer(matchMessage)) {
            logger.warn("feedMessageQueue is full");
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    private static class CandlePersistenceThread extends Thread {
        private final BlockingQueue<Candle> candleQueue;
        private final CandleRepository candleRepository;

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Candle candle = candleQueue.take();
                    candleRepository.save(candle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }
            logger.info("exiting...");
        }
    }

    @RequiredArgsConstructor
    @Slf4j
    private static class FeedMessagePublishThread extends Thread {
        private final MarketMessagePublisher marketMessagePublisher;
        private final TickerManager tickerManager;
        private final BlockingQueue<MarketMessage> feedMessageQueue;

        @Override
        public void run() {
            logger.info("starting...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    MarketMessage message = feedMessageQueue.take();
                    marketMessagePublisher.publish(message);

                    if (message instanceof TickerMessage) {
                        TickerMessage tickerMessage = (TickerMessage)message;
                        tickerManager.setTicker(tickerMessage.getProductId(), tickerMessage);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                }
            }
            logger.info("exiting...");
        }
    }
}
