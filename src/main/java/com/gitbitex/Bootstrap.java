package com.gitbitex;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.gitbitex.marketdata.AccountManager;
import com.gitbitex.marketdata.AccountantThread;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.CandleMakerThread;
import com.gitbitex.marketdata.OrderBookLogPublishThread;
import com.gitbitex.marketdata.OrderPersistenceThread;
import com.gitbitex.marketdata.OrderManager;
import com.gitbitex.marketdata.TickerManager;
import com.gitbitex.marketdata.TickerThread;
import com.gitbitex.marketdata.TradePersistenceThread;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.MatchingThread;
import com.gitbitex.matchingengine.command.MatchingEngineCommandDeserializer;
import com.gitbitex.matchingengine.log.LogDeserializer;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import com.gitbitex.product.repository.ProductRepository;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import com.gitbitex.support.kafka.KafkaProperties;
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
        startAccountant(appProperties.getAccountantThreadNum());

        List<String> productIds = productRepository.findAll().stream()
            .map(Product::getProductId)
            .collect(Collectors.toList());

        startMatchingEngine(productIds, 1);
        startOrderCommandSharding(productIds, 1);
        startCandleMaker(productIds, 1);
        startTicker(productIds, 1);
        startTradePersistence(productIds, 1);
        startOrderBookLogPublish(productIds, 1);
    }

    @PreDestroy
    public void destroy() {
        for (Thread thread : threads) {
            if (thread instanceof KafkaConsumerThread) {
                ((KafkaConsumerThread<?, ?>)thread).shutdown();
            }
        }
    }

    private void startAccountant(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Accountant";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                new LogDeserializer());
            AccountantThread accountantThread = new AccountantThread(consumer, accountManager, messageProducer,
                appProperties);
            accountantThread.setName(groupId + "-" + accountantThread.getId());
            accountantThread.start();
            threads.add(accountantThread);
        }
    }

    private void startMatchingEngine(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Matching";
            MatchingThread matchingThread = new MatchingThread(orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new MatchingEngineCommandDeserializer()),
                messageProducer, appProperties);
            matchingThread.setName(groupId + "-" + matchingThread.getId());
            matchingThread.start();
            threads.add(matchingThread);
        }
    }

    private void startOrderCommandSharding(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderCommandSharding";
            OrderPersistenceThread orderPersistenceThread = new OrderPersistenceThread(
                productIds,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new LogDeserializer()),
                messageProducer, appProperties, orderManager);
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

    private void startTicker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Ticker1";
            TickerThread tickerThread = new TickerThread(tickerManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new LogDeserializer()),
                appProperties);
            tickerThread.setName(groupId + "-" + tickerThread.getId());
            tickerThread.start();
            threads.add(tickerThread);
        }
    }

    private void startOrderBookLogPublish(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderBookLogPublish";
            TradePersistenceThread tradePersistenceThread = new TradePersistenceThread(tradeRepository,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new LogDeserializer()),
                appProperties);
            tradePersistenceThread.setName(groupId + "-" + tradePersistenceThread.getId());
            tradePersistenceThread.start();
            threads.add(tradePersistenceThread);
        }
    }

    private void startTradePersistence(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "TradePersistence";
            OrderBookLogPublishThread thread = new OrderBookLogPublishThread(productIds,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new StringDeserializer()),
                redissonClient, appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    public Properties getProperties(String groupId) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        properties.put("group.id", groupId);
        properties.put("enable.auto.commit", "false");
        properties.put("session.timeout.ms", "30000");
        properties.put("auto.offset.reset", "earliest");
        return properties;
    }
}
