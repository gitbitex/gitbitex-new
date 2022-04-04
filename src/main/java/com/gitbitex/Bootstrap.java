package com.gitbitex;

import com.gitbitex.accountant.AccountManager;
import com.gitbitex.accountant.AccountantThread;
import com.gitbitex.accountant.command.AccountCommandDeserializer;
import com.gitbitex.entity.Product;
import com.gitbitex.kafka.KafkaConsumerThread;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.*;
import com.gitbitex.matchingengine.command.OrderBookCommandDeserializer;
import com.gitbitex.matchingengine.log.OrderBookLogDeserializer;
import com.gitbitex.orderprocessor.OrderCommandShardingThread;
import com.gitbitex.orderprocessor.OrderPersistenceThread;
import com.gitbitex.orderprocessor.OrderManager;
import com.gitbitex.orderprocessor.TradePersistenceThread;
import com.gitbitex.orderprocessor.command.OrderCommandDeserializer;
import com.gitbitex.repository.CandleRepository;
import com.gitbitex.repository.ProductRepository;
import com.gitbitex.repository.TradeRepository;
import com.gitbitex.support.kafka.KafkaProperties;
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
    private final OrderBookSnapshotManager orderBookSnapshotManager;
    private final TradeRepository tradeRepository;
    private final ProductRepository productRepository;
    private final CandleRepository candleRepository;
    private final KafkaMessageProducer messageProducer;
    private final TickerManager tickerManager;
    private final MarketMessagePublisher marketMessagePublisher;
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final List<Thread> threads = new ArrayList<>();

    @PostConstruct
    public void init() {
        startAccountant(1);
        startOrderProcessor(1);

        for (Product product : productRepository.findAll()) {
            startMatchingEngine(product.getProductId(), 1);
            startOrderBookSnapshotTaker(product.getProductId(), 1);
            startOrderCommandSharding(product.getProductId(), 1);
            startMarketDataMaker(product.getProductId(), 1);
            startTradePersistence(product.getProductId(), 1);
        }
    }

    @PreDestroy
    public void destroy() {
        for (Thread thread : threads) {
            if (thread instanceof KafkaConsumerThread) {
                ((KafkaConsumerThread<?, ?>) thread).shutdown();
            }
        }
    }

    private void startAccountant(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Accountant";
            AccountantThread accountantThread = new AccountantThread(
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new AccountCommandDeserializer()),
                    accountManager, orderManager, productRepository, messageProducer, appProperties);
            accountantThread.setName(groupId + "-" + accountantThread.getId());
            accountantThread.start();
            threads.add(accountantThread);
        }
    }

    private void startOrderProcessor(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderProcessor";
            OrderPersistenceThread orderPersistenceThread = new OrderPersistenceThread(
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderCommandDeserializer()),
                    messageProducer, orderManager, appProperties);
            orderPersistenceThread.setName(groupId + "-" + orderPersistenceThread.getId());
            orderPersistenceThread.start();
            threads.add(orderPersistenceThread);
        }
    }

    private void startMatchingEngine(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Matching-" + productId;
            MatchingThread matchingThread = new MatchingThread(productId, orderBookSnapshotManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookCommandDeserializer()),
                    messageProducer, appProperties);
            matchingThread.setName(groupId + "-" + matchingThread.getId());
            matchingThread.start();
            threads.add(matchingThread);
        }
    }

    private void startOrderBookSnapshotTaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "SnapshotTaker-" + productId;
            OrderBookSnapshottingThread orderBookSnapshottingThread = new OrderBookSnapshottingThread(productId,
                    orderBookSnapshotManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                    marketMessagePublisher, appProperties);
            orderBookSnapshottingThread.setName(groupId + "-" + orderBookSnapshottingThread.getId());
            orderBookSnapshottingThread.start();
            threads.add(orderBookSnapshottingThread);
        }
    }

    private void startOrderCommandSharding(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderCommandSharding-" + productId;
            OrderCommandShardingThread orderCommandShardingThread = new OrderCommandShardingThread(productId,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                    messageProducer, appProperties);
            orderCommandShardingThread.setName(groupId + "-" + orderCommandShardingThread.getId());
            orderCommandShardingThread.start();
            threads.add(orderCommandShardingThread);
        }
    }

    private void startMarketDataMaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "MarketDataMaker-" + productId;
            MarketDataMakerThread marketDataMakerThread = new MarketDataMakerThread(productId, candleRepository,
                    tickerManager, marketMessagePublisher,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()), appProperties);
            marketDataMakerThread.setName(groupId + "-" + marketDataMakerThread.getId());
            marketDataMakerThread.start();
            threads.add(marketDataMakerThread);
        }
    }

    private void startTradePersistence(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "TradePersistence-" + productId;
            TradePersistenceThread tradePersistenceThread = new TradePersistenceThread(productId, tradeRepository,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
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
        return properties;
    }
}
