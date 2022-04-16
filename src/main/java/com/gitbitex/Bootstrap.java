package com.gitbitex;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.gitbitex.account.AccountManager;
import com.gitbitex.account.AccountantThread;
import com.gitbitex.account.command.AccountCommandDeserializer;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.CandleMakerThread;
import com.gitbitex.marketdata.OrderBookLogPublishLThread;
import com.gitbitex.marketdata.TickerManager;
import com.gitbitex.marketdata.TickerThread;
import com.gitbitex.marketdata.TradePersistenceThread;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.MatchingThread;
import com.gitbitex.matchingengine.command.OrderBookCommandDeserializer;
import com.gitbitex.matchingengine.log.OrderBookLogDeserializer;
import com.gitbitex.matchingengine.snapshot.FullOrderBookPersistenceThread;
import com.gitbitex.matchingengine.snapshot.L2BatchOrderBookPersistenceThread;
import com.gitbitex.matchingengine.snapshot.L2OrderBookPersistenceThread;
import com.gitbitex.matchingengine.snapshot.L3OrderBookPersistenceThread;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.order.OrderCommandShardingThread;
import com.gitbitex.order.OrderManager;
import com.gitbitex.order.OrderPersistenceThread;
import com.gitbitex.order.command.OrderCommandDeserializer;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import com.gitbitex.product.repository.ProductRepository;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import com.gitbitex.support.kafka.KafkaProperties;
import lombok.RequiredArgsConstructor;
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
        startOrderProcessor(appProperties.getOrderProcessorThreadNum());

        for (Product product : productRepository.findAll()) {
            startMatchingEngine(product.getProductId(), 1);
            startFullOrderBookSnapshotTaker(product.getProductId(), 1);
            startL2OrderBookSnapshotTaker(product.getProductId(), 1);
            startL3OrderBookSnapshotTaker(product.getProductId(), 1);
            startOrderCommandSharding(product.getProductId(), 1);
            startCandleMaker(product.getProductId(), 1);
            startTicker(product.getProductId(), 1);
            startTradePersistence(product.getProductId(), 1);
            startOrderBookLogPublish(product.getProductId(), 1);
            startL2OrderBookSnapshotTaker1(product.getProductId(), 1);
        }
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
            AccountantThread accountantThread = new AccountantThread(
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new AccountCommandDeserializer()),
                accountManager, messageProducer, appProperties);
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
            MatchingThread matchingThread = new MatchingThread(productId, orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new OrderBookCommandDeserializer()),
                messageProducer, appProperties);
            matchingThread.setName(groupId + "-" + matchingThread.getId());
            matchingThread.start();
            threads.add(matchingThread);
        }
    }

    private void startFullOrderBookSnapshotTaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-Full-" + productId;
            FullOrderBookPersistenceThread thread = new FullOrderBookPersistenceThread(productId,
                orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL2OrderBookSnapshotTaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L2-" + productId;
            L2OrderBookPersistenceThread thread = new L2OrderBookPersistenceThread(productId,
                orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL2OrderBookSnapshotTaker1(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L2Batch-" + productId;
            L2BatchOrderBookPersistenceThread thread = new L2BatchOrderBookPersistenceThread(productId,
                orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL3OrderBookSnapshotTaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L3-" + productId;
            L3OrderBookPersistenceThread thread = new L3OrderBookPersistenceThread(productId,
                orderBookManager,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
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

    private void startCandleMaker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "CandlerMaker-" + productId;
            CandleMakerThread candleMakerThread = new CandleMakerThread(productId, candleRepository,
                redissonClient,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new OrderBookLogDeserializer()), appProperties);
            candleMakerThread.setName(groupId + "-" + candleMakerThread.getId());
            candleMakerThread.start();
            threads.add(candleMakerThread);
        }
    }

    private void startTicker(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Ticker-" + productId;
            TickerThread tickerThread = new TickerThread(productId, tickerManager,
                redissonClient,
                new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                    new OrderBookLogDeserializer()), appProperties);
            tickerThread.setName(groupId + "-" + tickerThread.getId());
            tickerThread.start();
            threads.add(tickerThread);
        }
    }

    private void startOrderBookLogPublish(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderBookLogPublish-" + productId;
            TradePersistenceThread tradePersistenceThread = new TradePersistenceThread(productId, tradeRepository,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookLogDeserializer()),
                appProperties);
            tradePersistenceThread.setName(groupId + "-" + tradePersistenceThread.getId());
            tradePersistenceThread.start();
            threads.add(tradePersistenceThread);
        }
    }

    private void startTradePersistence(String productId, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "TradePersistence-" + productId;
            OrderBookLogPublishLThread thread = new OrderBookLogPublishLThread(productId,
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
