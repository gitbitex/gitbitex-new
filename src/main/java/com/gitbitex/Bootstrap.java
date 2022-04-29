package com.gitbitex;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.gitbitex.account.AccountManager;
import com.gitbitex.account.AccountantThread;
import com.gitbitex.account.command.AccountCommandDeserializer;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.*;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.MatchingThread;
import com.gitbitex.matchingengine.command.OrderBookCommandDeserializer;
import com.gitbitex.matchingengine.log.OrderBookLogDeserializer;
import com.gitbitex.matchingengine.snapshot.*;
import com.gitbitex.order.OrderCommandShardingThread;
import com.gitbitex.order.OrderManager;
import com.gitbitex.order.OrderPersistenceThread;
import com.gitbitex.order.command.OrderCommandDeserializer;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import com.gitbitex.product.repository.ProductRepository;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import com.gitbitex.support.kafka.KafkaProperties;
import com.gitbitex.support.metric.JsonSlf4Reporter;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final JsonSlf4Reporter jsonSlf4Reporter;
    private final MetricRegistry metricRegistry;
    private final List<Thread> threads = new ArrayList<>();

    @PostConstruct
    public void init() {
        jsonSlf4Reporter.start(1, TimeUnit.SECONDS);

        startAccountant(appProperties.getAccountantThreadNum());
        startOrderProcessor(appProperties.getOrderProcessorThreadNum());

        List<String> productIds = productRepository.findAll().stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        startMatchingEngine(productIds, 1);
        startFullOrderBookSnapshotTaker(productIds, 1);
        startL2OrderBookSnapshotTaker(productIds, 1);
        startL2BatchOrderBookSnapshot(productIds, 1);
        startL3OrderBookSnapshotTaker(productIds, 1);
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
                ((KafkaConsumerThread<?, ?>) thread).shutdown();
            }
        }
    }

    private void startAccountant(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Accountant";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new AccountCommandDeserializer());
            AccountantThread accountantThread = new AccountantThread(consumer, accountManager, messageProducer,
                    metricRegistry, appProperties);
            accountantThread.setName(groupId + "-" + accountantThread.getId());
            accountantThread.start();
            threads.add(accountantThread);
        }
    }

    private void startOrderProcessor(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderProcessor";
            var consumer = new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderCommandDeserializer());
            OrderPersistenceThread orderPersistenceThread = new OrderPersistenceThread(consumer,
                    messageProducer, orderManager, metricRegistry, appProperties);
            orderPersistenceThread.setName(groupId + "-" + orderPersistenceThread.getId());
            orderPersistenceThread.start();
            threads.add(orderPersistenceThread);
        }
    }

    private void startMatchingEngine(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Matching";
            MatchingThread matchingThread = new MatchingThread(productIds, orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(), new OrderBookCommandDeserializer()),
                    messageProducer, metricRegistry, appProperties);
            matchingThread.setName(groupId + "-" + matchingThread.getId());
            matchingThread.start();
            threads.add(matchingThread);
        }
    }

    private void startFullOrderBookSnapshotTaker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-Full";
            FullOrderBookSnapshotThread thread = new FullOrderBookSnapshotThread(productIds,
                    orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL2OrderBookSnapshotTaker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L2";
            L2OrderBookSnapshotThread thread = new L2OrderBookSnapshotThread(productIds,
                    orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL2BatchOrderBookSnapshot(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L2Batch";
            L2BatchOrderBookSnapshotThread thread = new L2BatchOrderBookSnapshotThread(productIds,
                    orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startL3OrderBookSnapshotTaker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Snapshot-L3";
            L3OrderBookSnapshotThread thread = new L3OrderBookSnapshotThread(productIds,
                    orderBookManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
                    appProperties);
            thread.setName(groupId + "-" + thread.getId());
            thread.start();
            threads.add(thread);
        }
    }

    private void startOrderCommandSharding(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderCommandSharding";
            OrderCommandShardingThread orderCommandShardingThread = new OrderCommandShardingThread(productIds,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
                    messageProducer, appProperties);
            orderCommandShardingThread.setName(groupId + "-" + orderCommandShardingThread.getId());
            orderCommandShardingThread.start();
            threads.add(orderCommandShardingThread);
        }
    }

    private void startCandleMaker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "CandlerMaker";
            CandleMakerThread candleMakerThread = new CandleMakerThread(productIds, candleRepository,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()), appProperties);
            candleMakerThread.setName(groupId + "-" + candleMakerThread.getId());
            candleMakerThread.start();
            threads.add(candleMakerThread);
        }
    }

    private void startTicker(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "Ticker";
            TickerThread tickerThread = new TickerThread(productIds, tickerManager,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()), appProperties);
            tickerThread.setName(groupId + "-" + tickerThread.getId());
            tickerThread.start();
            threads.add(tickerThread);
        }
    }

    private void startOrderBookLogPublish(List<String> productIds, int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            String groupId = "OrderBookLogPublish";
            TradePersistenceThread tradePersistenceThread = new TradePersistenceThread(productIds, tradeRepository,
                    new KafkaConsumer<>(getProperties(groupId), new StringDeserializer(),
                            new OrderBookLogDeserializer()),
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
