package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import com.gitbitex.matchingengine.message.OrderBookMessage;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.stripexecutor.StripedExecutorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook = new AccountBook();
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final ConcurrentHashMap<String, SimpleOrderBook> simpleOrderBooks = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, ModifiedObjectList> stateUnsavedModifiedObjects
            = new ConcurrentSkipListMap<>();
    private final StripedExecutorService simpleOrderBookExecutor = new StripedExecutorService(1);
    private final StripedExecutorService kafkaExecutor = new StripedExecutorService(20);
    private final StripedExecutorService redisExecutor = new StripedExecutorService(20);
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(3);
    private final OrderBookManager orderBookManager;
    private final KafkaMessageProducer producer;
    private final EngineStateStore stateStore;
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic orderBookMessageTopic;
    private final ConcurrentLinkedQueue<ModifiedObjectList> modifiedObjectListQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong modifiedObjectListQueueSizeCounter = new AtomicLong();
    private final ConcurrentHashMap<String, Long> lastL2OrderBookSequences = new ConcurrentHashMap<>();
    private final Counter commandProcessedCounter;
    private final Counter modifiedObjectCreatedCounter;
    private final Counter modifiedObjectSavedCounter;
    @Getter
    private Long startupCommandOffset;

    public MatchingEngine(EngineStateStore stateStore, KafkaMessageProducer producer,
                          RedissonClient redissonClient, OrderBookManager orderBookManager) {
        this.producer = producer;
        this.stateStore = stateStore;
        this.orderBookManager = orderBookManager;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.orderBookMessageTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
        this.commandProcessedCounter = Counter.builder("gbe.matching-engine.command.processed")
                .register(Metrics.globalRegistry);
        this.modifiedObjectCreatedCounter = Counter.builder("gbe.matching-engine.modified-object.created")
                .register(Metrics.globalRegistry);
        this.modifiedObjectSavedCounter = Counter.builder("gbe.matching-engine.modified-object.saved")
                .register(Metrics.globalRegistry);
        Gauge.builder("gbe.matching-engine.state-unsaved-modified-object-map.size",
                stateUnsavedModifiedObjects::size).register(Metrics.globalRegistry);
        Gauge.builder("gbe.matching-engine.modified-object-list-queue.size", modifiedObjectListQueueSizeCounter::get)
                .register(Metrics.globalRegistry);
        restoreState();
        startSateSaveTask();
        startModifiedObjectSaveTask();
        startL2OrderBookPublishTask();
    }

    public void shutdown() {
        simpleOrderBookExecutor.shutdown();
        kafkaExecutor.shutdown();
        redisExecutor.shutdown();
        scheduledExecutor.shutdown();
    }

    public void executeCommand(DepositCommand command) {
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), null);
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(), command.getTransactionId(),
                modifiedObjects);
        enqueue(modifiedObjects);
        commandProcessedCounter.increment();
    }

    public void executeCommand(PutProductCommand command) {
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), null);
        productBook.putProduct(new Product(command), modifiedObjects);
        enqueue(modifiedObjects);
        commandProcessedCounter.increment();
    }

    public void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = createOrderBook(command.getProductId());
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), command.getProductId());
        orderBook.placeOrder(new Order(command), modifiedObjects);
        enqueue(modifiedObjects);
        commandProcessedCounter.increment();
    }

    public void executeCommand(CancelOrderCommand command) {
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook != null) {
            ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), command.getProductId());
            orderBook.cancelOrder(command.getOrderId(), modifiedObjects);
            enqueue(modifiedObjects);
        }
        commandProcessedCounter.increment();
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, null, null, accountBook, productBook);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    private void enqueue(ModifiedObjectList modifiedObjects) {
        modifiedObjectCreatedCounter.increment(modifiedObjects.size());

        while (modifiedObjectListQueueSizeCounter.get() >= 1000000) {
            logger.warn("modified object queue is full");
            Thread.yield();
        }

        modifiedObjectListQueue.offer(modifiedObjects);
        modifiedObjectListQueueSizeCounter.incrementAndGet();
    }

    private void save(ModifiedObjectList modifiedObjects) {
        stateUnsavedModifiedObjects.put(modifiedObjects.getCommandOffset(), modifiedObjects);

        modifiedObjects.forEach(obj -> {
            if (obj instanceof Order) {
                save(modifiedObjects.getSavedCounter(), (Order) obj);
            } else if (obj instanceof Account) {
                save(modifiedObjects.getSavedCounter(), (Account) obj);
            } else if (obj instanceof Trade) {
                save(modifiedObjects.getSavedCounter(), (Trade) obj);
            } else if (obj instanceof OrderBookMessage) {
                save(modifiedObjects.getSavedCounter(), (OrderBookMessage) obj);
            } else {
                modifiedObjects.getSavedCounter().incrementAndGet();
                modifiedObjectSavedCounter.increment();
            }
        });

        updateSimpleOrderBook(modifiedObjects);
    }

    private void updateSimpleOrderBook(ModifiedObjectList modifiedObjects) {
        String productId = modifiedObjects.getProductId();
        if (productId == null) {
            return;
        }

        simpleOrderBookExecutor.execute(productId, () -> {
            simpleOrderBooks.putIfAbsent(productId, new SimpleOrderBook(productId));
            SimpleOrderBook simpleOrderBook = simpleOrderBooks.get(productId);
            modifiedObjects.forEach(obj -> {
                if (obj instanceof Order) {
                    Order order = (Order) obj;
                    if (order.getStatus() == OrderStatus.OPEN) {
                        simpleOrderBook.putOrder(order);
                    } else {
                        simpleOrderBook.removeOrder(order);
                    }
                } else if (obj instanceof OrderBookMessage) {
                    OrderBookMessage orderBookMessage = (OrderBookMessage) obj;
                    simpleOrderBook.setSequence(orderBookMessage.getSequence());
                } else if (obj instanceof OrderBookCompleteNotify) {
                    takeL2OrderBookSnapshot(simpleOrderBook, 200);
                }
            });
        });
    }

    private void takeL2OrderBookSnapshot(SimpleOrderBook simpleOrderBook, long delta) {
        String productId = simpleOrderBook.getProductId();
        long lastL2OrderBookSequence = lastL2OrderBookSequences.getOrDefault(productId, 0L);
        if (simpleOrderBook.getSequence() - lastL2OrderBookSequence > delta) {
            L2OrderBook l2OrderBook = new L2OrderBook(simpleOrderBook, 25);
            lastL2OrderBookSequences.put(productId, simpleOrderBook.getSequence());
            orderBookManager.saveL2BatchOrderBook(l2OrderBook);
        }
    }

    private void save(AtomicLong savedCounter, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.sendAccount(account, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
            redisExecutor.execute(account.getUserId(), () -> {
                accountTopic.publishAsync(data);
            });
        });
    }

    private void save(AtomicLong savedCounter, Order order) {
        String productId = order.getProductId();
        kafkaExecutor.execute(productId, () -> {
            String data = JSON.toJSONString(order);
            producer.sendOrder(order, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
            redisExecutor.execute(order.getUserId(), () -> {
                orderTopic.publishAsync(data);
            });
        });
    }

    private void save(AtomicLong savedCounter, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            producer.sendTrade(trade, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
        });
    }

    private void save(AtomicLong savedCounter, OrderBookMessage orderBookMessage) {
        savedCounter.incrementAndGet();
        modifiedObjectSavedCounter.increment();
        String productId = orderBookMessage.getProductId();
        redisExecutor.execute(productId, () -> {
            orderBookMessageTopic.publishAsync(JSON.toJSONString(orderBookMessage));
        });
    }

    private void saveState() {
        if (stateUnsavedModifiedObjects.isEmpty()) {
            return;
        }

        Long commandOffset = null;
        Map<String, Account> accounts = new HashMap<>();
        Map<String, Order> orders = new HashMap<>();
        Map<String, Product> products = new HashMap<>();
        Map<String, Long> tradeIds = new HashMap<>();
        Map<String, Long> sequences = new HashMap<>();

        Iterator<Entry<Long, ModifiedObjectList>> itr = stateUnsavedModifiedObjects.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Long, ModifiedObjectList> entry = itr.next();
            ModifiedObjectList modifiedObjects = entry.getValue();
            if (!modifiedObjects.allSaved()) {
                break;
            }
            for (Object obj : modifiedObjects) {
                if (obj instanceof Account) {
                    Account account = (Account) obj;
                    accounts.put(account.getId(), account);
                } else if (obj instanceof Order) {
                    Order order = (Order) obj;
                    orders.put(order.getId(), order);
                } else if (obj instanceof Trade) {
                    Trade trade = (Trade) obj;
                    tradeIds.put(trade.getProductId(), trade.getTradeId());
                } else if (obj instanceof OrderBookMessage) {
                    OrderBookMessage orderBookMessage = (OrderBookMessage) obj;
                    sequences.put(orderBookMessage.getProductId(), orderBookMessage.getSequence());
                } else if (obj instanceof Product) {
                    Product product = (Product) obj;
                    products.put(product.getId(), product);
                }
            }
            commandOffset = entry.getKey();
            itr.remove();
        }

        if (commandOffset != null) {
            Long savedCommandOffset = stateStore.getCommandOffset();
            if (savedCommandOffset != null && commandOffset <= savedCommandOffset) {
                logger.warn("ignore outdated commandOffset: ignored={} saved={}", commandOffset, savedCommandOffset);
                return;
            }
            stateStore.save(commandOffset, accounts.values(), orders.values(), products.values(), tradeIds, sequences);
            logger.info(
                    "state saved: commandOffset={}, {} account(s), {} order(s), {} product(s), {} tradeId(s), {} sequence"
                            + "(s)",
                    commandOffset, accounts.size(), orders.size(), products.size(), tradeIds.size(), sequences.size());
        }
    }

    private void restoreState() {
        startupCommandOffset = stateStore.getCommandOffset();
        if (startupCommandOffset == null) {
            return;
        }
        stateStore.getProducts().forEach(productBook::addProduct);
        stateStore.getAccounts().forEach(accountBook::add);
        stateStore.getOrderBookStates().forEach(x -> {
            orderBooks.put(x.getId(),
                    new OrderBook(x.getId(), x.getTradeId(), x.getSequence(), accountBook, productBook));
            simpleOrderBooks.put(x.getId(), new SimpleOrderBook(x.getId(), x.getSequence()));
        });
        stateStore.getOrders().forEach(x -> {
            orderBooks.get(x.getProductId()).addOrder(x);
            simpleOrderBooks.get(x.getProductId()).putOrder(x);
        });
    }

    private void startSateSaveTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                saveState();
            } catch (Exception e) {
                logger.error("save state error: {}", e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void startModifiedObjectSaveTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (true) {
                ModifiedObjectList modifiedObjects = modifiedObjectListQueue.poll();
                if (modifiedObjects == null) {
                    break;
                }
                modifiedObjectListQueueSizeCounter.decrementAndGet();
                save(modifiedObjects);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public void startL2OrderBookPublishTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            simpleOrderBooks.forEach((productId, simpleOrderBook) -> {
                simpleOrderBookExecutor.execute(productId, () -> {
                    takeL2OrderBookSnapshot(simpleOrderBook, 0);
                });
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

}
