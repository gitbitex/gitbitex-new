package com.gitbitex;

import com.gitbitex.marketdata.*;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.marketdata.manager.TickerManager;
import com.gitbitex.marketdata.manager.TradeManager;
import com.gitbitex.marketdata.orderbook.OrderBookSnapshotManager;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.MatchingEngineThread;
import com.gitbitex.matchingengine.MessageSender;
import com.gitbitex.matchingengine.command.CommandDeserializer;
import com.gitbitex.matchingengine.message.MatchingEngineMessageDeserializer;
import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import com.gitbitex.matchingengine.snapshot.MatchingEngineSnapshotThread;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import com.gitbitex.middleware.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class Bootstrap {
    private final OrderManager orderManager;
    private final AccountManager accountManager;
    private final TradeRepository tradeRepository;
    private final TradeManager tradeManager;
    private final CandleRepository candleRepository;
    private final TickerManager tickerManager;
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final List<Thread> threads = new ArrayList<>();
    private final EngineSnapshotManager engineSnapshotManager;
    private final MessageSender messageSender;
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final RedissonClient redissonClient;

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

    @PreDestroy
    public void destroy() {
        for (Thread thread : threads) {
            if (thread instanceof KafkaConsumerThread) {
                ((KafkaConsumerThread<?, ?>) thread).shutdown();
            }
        }
    }

    private void startMatchingEngine(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "MatchingEngine";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new CommandDeserializer());
            var matchingEngineThread = new MatchingEngineThread(consumer, engineSnapshotManager, messageSender, appProperties);
            matchingEngineThread.setName(groupId + "-" + matchingEngineThread.getId());
            matchingEngineThread.start();
            threads.add(matchingEngineThread);
        }
    }

    private void startSnapshotThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "EngineSnapshot";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var thread = new MatchingEngineSnapshotThread(consumer, engineSnapshotManager, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startOrderBookSnapshotThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderBookSnapshot";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var thread = new OrderBookSnapshotThread(consumer, orderBookSnapshotManager, engineSnapshotManager, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startAccountPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Account";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var accountPersistenceThread = new AccountPersistenceThread(consumer, accountManager, redissonClient,
                    appProperties);
            accountPersistenceThread.setName(groupId + "-" + accountPersistenceThread.getId());
            accountPersistenceThread.start();
            threads.add(accountPersistenceThread);
        }
    }

    private void startTickerThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Ticker";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var tickerThread = new TickerThread(consumer, tickerManager, appProperties);
            tickerThread.setName(groupId + "-" + tickerThread.getId());
            tickerThread.start();
            threads.add(tickerThread);
        }
    }

    private void startOrderPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Order";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var orderPersistenceThread = new OrderPersistenceThread(consumer, orderManager, redissonClient, appProperties);
            orderPersistenceThread.setName(groupId + "-" + orderPersistenceThread.getId());
            orderPersistenceThread.start();
            threads.add(orderPersistenceThread);
        }
    }

    private void startCandleMaker(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "CandlerMaker";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var candleMakerThread = new CandleMakerThread(consumer, candleRepository, appProperties);
            candleMakerThread.setName(groupId + "-" + candleMakerThread.getId());
            candleMakerThread.start();
            threads.add(candleMakerThread);
        }
    }

    private void startTradePersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Trade1";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new MatchingEngineMessageDeserializer());
            var tradePersistenceThread = new TradePersistenceThread(consumer, tradeManager, redissonClient, appProperties);
            tradePersistenceThread.setName(groupId + "-" + tradePersistenceThread.getId());
            tradePersistenceThread.start();
            threads.add(tradePersistenceThread);
        }
    }

    public Properties getProperties(String groupId) {
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
