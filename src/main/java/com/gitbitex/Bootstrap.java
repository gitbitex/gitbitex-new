package com.gitbitex;

import com.gitbitex.marketdata.*;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.marketdata.manager.TickerManager;
import com.gitbitex.marketdata.manager.TradeManager;
import com.gitbitex.marketdata.orderbook.OrderBookSnapshotManager;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.matchingengine.MatchingEngineThread;
import com.gitbitex.matchingengine.MessageSender;
import com.gitbitex.matchingengine.command.Command;
import com.gitbitex.matchingengine.command.CommandDeserializer;
import com.gitbitex.matchingengine.message.MatchingEngineMessageDeserializer;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import com.gitbitex.matchingengine.snapshot.MatchingEngineSnapshotThread;
import com.gitbitex.middleware.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class Bootstrap {
    private final OrderManager orderManager;
    private final AccountManager accountManager;
    private final TradeManager tradeManager;
    private final CandleRepository candleRepository;
    private final TickerManager tickerManager;
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final EngineSnapshotManager engineSnapshotManager;
    private final MessageSender messageSender;
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final RedissonClient redissonClient;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

    @PostConstruct
    public void init() {
        startMatchingEngine(1);
        startOrderPersistenceThread(1);
        startTradePersistenceThread(1);
        startAccountPersistenceThread(1);
        startCandleMaker(1);
        startTickerThread(1);
        startSnapshotThread(1);
        startOrderBookSnapshotThread(1);
    }

    private void startMatchingEngine(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "MatchingEngine";
            var consumer = getEngineCommandKafkaConsumer(groupId);
            var thread = new MatchingEngineThread(consumer, engineSnapshotManager, messageSender, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() -> startMatchingEngine(1)));
            thread.start();
        }
    }

    private void startSnapshotThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "EngineSnapshot";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new MatchingEngineSnapshotThread(consumer, engineSnapshotManager, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() -> startSnapshotThread(1)));
            thread.start();
        }
    }

    private void startOrderBookSnapshotThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderBookSnapshot";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new OrderBookSnapshotThread(consumer, orderBookSnapshotManager, engineSnapshotManager,
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() ->
                    startOrderBookSnapshotThread(1)));
            thread.start();
        }
    }

    private void startAccountPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Account";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new AccountPersistenceThread(consumer, accountManager, redissonClient,
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() ->
                    startAccountPersistenceThread(1)));
            thread.start();
        }
    }

    private void startTickerThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Ticker";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new TickerThread(consumer, tickerManager, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() -> startTickerThread(1)));
            thread.start();
        }
    }

    private void startOrderPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Order";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new OrderPersistenceThread(consumer, orderManager, redissonClient, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() ->
                    startOrderPersistenceThread(1)));
            thread.start();
        }
    }

    private void startCandleMaker(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "CandlerMaker";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new CandleMakerThread(consumer, candleRepository, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() -> startCandleMaker(1)));
            thread.start();
        }
    }

    private void startTradePersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Trade1";
            var consumer = getEngineMessageKafkaConsumer(groupId);
            var thread = new TradePersistenceThread(consumer, tradeManager, redissonClient, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler(() ->
                    startTradePersistenceThread(1)));
            thread.start();
        }
    }

    private Thread.UncaughtExceptionHandler getUncaughtExceptionHandler(Runnable runnable) {
        return (t, ex) -> {
            logger.error("Thread {} triggered an uncaught exception and will start a new thread in " +
                    "3 seconds, ex:{}", t.getName(), ex.getMessage(), ex);
            executor.schedule(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    logger.error("start thread failed", e);
                }
            }, 3, java.util.concurrent.TimeUnit.SECONDS);
        };
    }

    private KafkaConsumer<String, Message> getEngineMessageKafkaConsumer(String groupId) {
        return new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                new MatchingEngineMessageDeserializer());
    }

    private KafkaConsumer<String, Command> getEngineCommandKafkaConsumer(String groupId) {
        return new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new CommandDeserializer());
    }

    private Properties getProperties(String groupId) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put("group.id", groupId);
        properties.put("enable.auto.commit", "false");
        properties.put("session.timeout.ms", "30000");
        properties.put("auto.offset.reset", "earliest");
        properties.put("max.poll.records", 2000);
        return properties;
    }
}
