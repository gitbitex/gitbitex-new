package com.gitbitex;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.AccountPersistenceThread;
import com.gitbitex.marketdata.CandleMakerThread;
import com.gitbitex.marketdata.OrderPersistenceThread;
import com.gitbitex.marketdata.TradePersistenceThread;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.marketdata.manager.OrderManager;
import com.gitbitex.marketdata.manager.ProductManager;
import com.gitbitex.marketdata.manager.TickerManager;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.ProductRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.MatchingThread;
import com.gitbitex.matchingengine.command.MatchingEngineCommandDeserializer;
import com.gitbitex.matchingengine.log.AccountMessageDeserializer;
import com.gitbitex.matchingengine.log.LogDeserializer;
import com.gitbitex.matchingengine.log.OrderMessageDeserializer;
import com.gitbitex.matchingengine.log.TradeMessageDeserializer;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.middleware.kafka.KafkaConsumerThread;
import com.gitbitex.middleware.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Bootstrap {
    private final OrderManager orderManager;
    private final AccountManager accountManager;
    private final OrderBookManager orderBookManager;
    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final ProductManager productManager;
    private final CandleRepository candleRepository;
    private final KafkaMessageProducer messageProducer;
    private final TickerManager tickerManager;
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final RedissonClient redissonClient;
    private final List<Thread> threads = new ArrayList<>();

    @PostConstruct
    public void init() {
        startMatchingEngine(1);
        startOrderPersistenceThread(1);
        //startTradePersistenceThread(1);
        startAccountPersistenceThread(appProperties.getAccountantThreadNum());
    }

    @PreDestroy
    public void destroy() {
        for (Thread thread : threads) {
            if (thread instanceof KafkaConsumerThread) {
                ((KafkaConsumerThread<?, ?>)thread).shutdown();
            }
        }
    }

    private void startAccountPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Account";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                new AccountMessageDeserializer());
            AccountPersistenceThread accountPersistenceThread = new AccountPersistenceThread(consumer, accountManager,
                messageProducer, appProperties);
            accountPersistenceThread.setName(groupId + "-" + accountPersistenceThread.getId());
            accountPersistenceThread.start();
            threads.add(accountPersistenceThread);
        }
    }

    private void startMatchingEngine(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Matching";
            MatchingThread matchingThread = new MatchingThread(orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new MatchingEngineCommandDeserializer()),
                messageProducer, redissonClient, appProperties);
            matchingThread.setName(groupId + "-" + matchingThread.getId());
            matchingThread.start();
            threads.add(matchingThread);
        }
    }

    private void startOrderPersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Order";
            OrderPersistenceThread orderPersistenceThread = new OrderPersistenceThread(
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderMessageDeserializer()),
                appProperties, orderManager);
            orderPersistenceThread.setName(groupId + "-" + orderPersistenceThread.getId());
            orderPersistenceThread.start();
            threads.add(orderPersistenceThread);
        }
    }

    private void startCandleMaker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "CandlerMaker1";
            CandleMakerThread candleMakerThread = new CandleMakerThread(productIds, candleRepository,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new LogDeserializer()), appProperties);
            candleMakerThread.setName(groupId + "-" + candleMakerThread.getId());
            candleMakerThread.start();
            threads.add(candleMakerThread);

        }
    }

    private void startTradePersistenceThread(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Trade";
            TradePersistenceThread tradePersistenceThread = new TradePersistenceThread(tradeRepository,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new TradeMessageDeserializer()),
                appProperties);
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
        properties.put("max.poll.records", 10000);
        return properties;
    }
}
