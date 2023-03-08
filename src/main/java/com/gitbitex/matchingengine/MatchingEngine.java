package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import com.gitbitex.matchingengine.log.OrderLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook = new AccountBook();
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final ConcurrentHashMap<String, SimpleOrderBook> simpleOrderBooks = new ConcurrentHashMap<>();
    private final StripedExecutorService simpleOrderBookExecutor = new StripedExecutorService(1);
    private final ConcurrentSkipListMap<Long, ModifiedObjectList<Object>> modifiedObjectsByCommandOffset = new ConcurrentSkipListMap<>();
    private final ExecutorService saveExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1000000));
    private final ScheduledExecutorService snapshotExecutor = Executors.newScheduledThreadPool(1);
    private final StripedExecutorService kafkaExecutor = new StripedExecutorService(2);
    private final StripedExecutorService redisExecutor = new StripedExecutorService(2);
    private final OrderBookManager orderBookManager;
    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final MatchingEngineStateStore stateStore;
    private final RTopic tradeTopic;
    private final RTopic orderBookLogTopic;
    private final ConcurrentLinkedQueue<ModifiedObjectList<Object>> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong queueSizeCounter = new AtomicLong();
    @Getter
    private Long lastCommandOffset;

    public MatchingEngine(MatchingEngineStateStore stateStore, KafkaMessageProducer producer, RedissonClient redissonClient, OrderBookManager orderBookManager, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.stateStore = stateStore;
        this.orderBookManager = orderBookManager;
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
        this.orderBookLogTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
        restoreState();
        startSateSaveTask();
        startModifiedObjectSaveTask();
    }

    public void executeCommand(DepositCommand command) {
        ModifiedObjectList<Object> modifiedObjects = new ModifiedObjectList<>(command.getOffset());
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(), command.getTransactionId(), modifiedObjects);
        enqueue(modifiedObjects);
    }

    public void executeCommand(PutProductCommand command) {
        ModifiedObjectList<Object> modifiedObjects = new ModifiedObjectList<>(command.getOffset());
        productBook.putProduct(new Product(command), modifiedObjects);
        enqueue(modifiedObjects);
    }

    public void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = createOrderBook(command.getProductId());
        ModifiedObjectList<Object> modifiedObjects = new ModifiedObjectList<>(command.getOffset());
        orderBook.placeOrder(new Order(command), modifiedObjects);
        enqueue(modifiedObjects);
    }

    public void executeCommand(CancelOrderCommand command) {
        ModifiedObjectList<Object> modifiedObjects = new ModifiedObjectList<>(command.getOffset());
        orderBooks.get(command.getProductId()).cancelOrder(command.getOrderId(), modifiedObjects);
        enqueue(modifiedObjects);
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, null, null, accountBook, productBook);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    private void enqueue(ModifiedObjectList<Object> modifiedObjects) {
        while (queueSizeCounter.get() >= 1000000) {
            logger.warn("queue is full");
            Thread.yield();
        }
        concurrentLinkedQueue.offer(modifiedObjects);
        queueSizeCounter.incrementAndGet();
    }

    private void save(ModifiedObjectList<Object> modifiedObjects) {
        modifiedObjectsByCommandOffset.put(modifiedObjects.getCommandOffset(), modifiedObjects);
        modifiedObjects.forEach(obj -> {
            if (obj instanceof Product) {
                modifiedObjects.getSavedCount().incrementAndGet();
            } else if (obj instanceof Order) {
                save(modifiedObjects.getSavedCount(), (Order) obj);
            } else if (obj instanceof Account) {
                save(modifiedObjects.getSavedCount(), (Account) obj);
            } else if (obj instanceof Trade) {
                save(modifiedObjects.getSavedCount(), (Trade) obj);
            } else if (obj instanceof OrderLog) {
                modifiedObjects.getSavedCount().incrementAndGet();
                save(modifiedObjects.getSavedCount(), (OrderLog) obj);
            } else if (obj instanceof OrderBookCompleteNotify) {
                modifiedObjects.getSavedCount().incrementAndGet();
                save(modifiedObjects.getSavedCount(), (OrderBookCompleteNotify) obj);
            }
        });
    }

    private void save(AtomicLong savedCounter, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(), data), (m, e) -> {
                if (e != null) {
                    throw new RuntimeException(e);
                }
                savedCounter.incrementAndGet();
            });
        });
    }

    private void save(AtomicLong savedCounter, Order order) {
        String productId = order.getProductId();
        kafkaExecutor.execute(productId, () -> {
            String data = JSON.toJSONString(order);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), productId, data), (m, e) -> {
                if (e != null) {
                    throw new RuntimeException(e);
                }
                savedCounter.incrementAndGet();
            });
        });
        updateOrderBook(order);
    }

    private void save(AtomicLong savedCounter, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data), (m, e) -> {
                if (e != null) {
                    throw new RuntimeException(e);
                }
                savedCounter.incrementAndGet();
            });
        });
    }

    private void save(AtomicLong savedCounter, OrderLog orderLog) {
        String productId = orderLog.getProductId();
        redisExecutor.execute(productId, () -> {
            String data = JSON.toJSONString(orderLog);
            //logger.info(data);
            orderBookLogTopic.publishAsync(data);
        });
        updateOrderBook(orderLog);
    }

    private void save(AtomicLong savedCounter, OrderBookCompleteNotify orderBookCompleteNotify) {
        updateOrderBook(orderBookCompleteNotify);
    }

    private void updateOrderBook(Order order) {
        String productId = order.getProductId();
        simpleOrderBookExecutor.execute(productId, () -> {
            simpleOrderBooks.putIfAbsent(productId, new SimpleOrderBook(productId));
            SimpleOrderBook simpleOrderBook = simpleOrderBooks.get(productId);
            if (order.getStatus() == OrderStatus.OPEN) {
                simpleOrderBook.putOrder(order);
            } else {
                simpleOrderBook.removeOrder(order);
            }
        });
    }

    private void updateOrderBook(OrderLog orderLog) {
        String productId = orderLog.getProductId();
        simpleOrderBookExecutor.execute(productId, () -> {
            simpleOrderBooks.putIfAbsent(productId, new SimpleOrderBook(productId));
            simpleOrderBooks.get(productId).setSequence(orderLog.getSequence());
        });
    }

    private void updateOrderBook(OrderBookCompleteNotify orderBookCompleteNotify) {
        String productId = orderBookCompleteNotify.getProductId();
        simpleOrderBookExecutor.execute(productId, () -> {
            simpleOrderBooks.putIfAbsent(productId, new SimpleOrderBook(productId));
            SimpleOrderBook simpleOrderBook = simpleOrderBooks.get(productId);
            if (simpleOrderBook != null) {
                //logger.info("take orderbook snapshot: {} {}",commandOffset,simpleOrderBook.getSequence());
                //L2OrderBook l2OrderBook = new L2OrderBook(simpleOrderBook);
                //l2OrderBook.setCommandOffset(commandOffset);
                //logger.info(JSON.toJSONString(l2OrderBook));
                //orderBookManager.saveL2BatchOrderBook(l2OrderBook);
            }
        });
    }

    private void saveState() {
        if (modifiedObjectsByCommandOffset.isEmpty()) {
            return;
        }

        Long commandOffset = null;
        Set<Account> accounts = new HashSet<>();
        Set<Order> orders = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Map<String, Long> tradeIdByProductId = new HashMap<>();
        Map<String, Long> sequenceByProductId = new HashMap<>();

        Iterator<Entry<Long, ModifiedObjectList<Object>>> itr = modifiedObjectsByCommandOffset.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Long, ModifiedObjectList<Object>> entry = itr.next();
            ModifiedObjectList<Object> modifiedObjects = entry.getValue();
            if (!modifiedObjects.isAllSaved()) {
                break;
            }
            for (Object obj : modifiedObjects) {
                if (obj instanceof Account) {
                    accounts.add((Account) obj);
                } else if (obj instanceof Order) {
                    orders.add((Order) obj);
                } else if (obj instanceof Trade) {
                    Trade trade = (Trade) obj;
                    tradeIdByProductId.put(trade.getProductId(), trade.getTradeId());
                } else if (obj instanceof OrderLog) {
                    OrderLog orderLog = (OrderLog) obj;
                    sequenceByProductId.put(orderLog.getProductId(), orderLog.getSequence());
                } else if (obj instanceof Product) {
                    products.add((Product) obj);
                }
            }
            commandOffset = entry.getKey();
            itr.remove();
        }

        if (commandOffset != null) {
            stateStore.write(commandOffset, accounts, orders, products, tradeIdByProductId, sequenceByProductId);
        }
    }

    private void restoreState() {
        lastCommandOffset = stateStore.getCommandOffset();
        if (lastCommandOffset != null) {
            Map<String, Long> tradeIds = stateStore.getTradeIds();
            Map<String, Long> sequences = stateStore.getSequences();
            stateStore.forEachProduct(productBook::addProduct);
            stateStore.forEachAccount(accountBook::add);
            stateStore.forEachOrder(order -> {
                String productId = order.getProductId();
                Long tradeId = tradeIds.get(productId);
                Long sequence = sequences.get(productId);
                orderBooks.computeIfAbsent(productId, k -> new OrderBook(productId, tradeId, sequence, accountBook, productBook)).addOrder(order);
                simpleOrderBooks.computeIfAbsent(productId, k -> new SimpleOrderBook(productId, sequence)).putOrder(order);
            });
        }
    }

    private void startSateSaveTask() {
        snapshotExecutor.scheduleAtFixedRate(() -> {
            try {
                saveState();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void startModifiedObjectSaveTask() {
        Executors.newFixedThreadPool(1).execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ModifiedObjectList<Object> modifiedObjects = concurrentLinkedQueue.poll();
                if (modifiedObjects == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                queueSizeCounter.decrementAndGet();
                save(modifiedObjects);
            }
        });
    }

}
