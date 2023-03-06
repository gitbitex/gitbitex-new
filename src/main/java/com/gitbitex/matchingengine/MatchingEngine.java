package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.log.OrderLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook;
    @Getter
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final Map<String, SimpleOrderBook> simpleOrderBooks = new HashMap<>();
    private final StripedExecutorService simpleOrderBookExecutor = new StripedExecutorService(2);
    private final ConcurrentSkipListMap<Long, ModifiedObjectList<Object>> modifiedObjectsByCommandOffset
            = new ConcurrentSkipListMap<>();
    private final ExecutorService saveExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100000));
    private final ScheduledExecutorService snapshotExecutor = Executors.newScheduledThreadPool(1);
    private final StripedExecutorService kafkaExecutor = new StripedExecutorService(50);
    private final StripedExecutorService redisExecutor = new StripedExecutorService(2);
    KafkaMessageProducer producer;
    RedissonClient redissonClient;
    AppProperties appProperties;
    MatchingEngineStateStore matchingEngineStateStore;

    public MatchingEngine(MatchingEngineStateStore matchingEngineStateStore, KafkaMessageProducer producer,
                          RedissonClient redissonClient, AppProperties appProperties) {
        this.accountBook = new AccountBook();
        this.appProperties = appProperties;
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.matchingEngineStateStore = matchingEngineStateStore;

        Long commandOffset = matchingEngineStateStore.getCommandOffset();
        if (commandOffset == null) {
            Map<String, Long> tradeIds = matchingEngineStateStore.getTradeIds();
            Map<String, Long> sequences = matchingEngineStateStore.getSequences();
            matchingEngineStateStore.forEachAccount(accountBook::add);
            matchingEngineStateStore.forEachOrder(order -> {
                String productId = order.getProductId();
                Long tradeId = tradeIds.get(productId);
                Long sequence = sequences.get(productId);
                orderBooks.computeIfAbsent(productId,
                                k -> new OrderBook(productId, tradeId, sequence, accountBook, productBook))
                        .addOrder(order);
                simpleOrderBooks.computeIfAbsent(productId,
                                k -> new SimpleOrderBook(productId, sequences.get(productId)))
                        .putOrder(order);
            });
        }

        snapshotExecutor.scheduleAtFixedRate(() -> {
            try {
                saveState();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void executeCommand(DepositCommand command) {
        ModifiedObjectList<Object> modifiedObjects = accountBook.deposit(command.getUserId(), command.getCurrency(),
                command.getAmount(), command.getTransactionId(), command.getOffset());
        save(command.getOffset(), modifiedObjects);
    }

    public void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = createOrderBook(command.getProductId());
        ModifiedObjectList<Object> modifiedObjects = new ModifiedObjectList<>();
        orderBook.placeOrder(new Order(command), modifiedObjects);
        save(command.getOffset(), modifiedObjects);
    }

    public void executeCommand(CancelOrderCommand command) {
        orderBooks.get(command.getProductId()).cancelOrder(command.getOrderId(), command.getOffset());
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, null, null, accountBook, productBook);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    private void save(Long commandOffset, ModifiedObjectList<Object> dirtyObjects) {
        modifiedObjectsByCommandOffset.put(commandOffset, dirtyObjects);
        saveExecutor.execute(() -> {
            for (Object dirtyObject : dirtyObjects) {
                if (dirtyObject instanceof Order) {
                    save(commandOffset, (Order) dirtyObject);
                } else if (dirtyObject instanceof Account) {
                    save(commandOffset, (Account) dirtyObject);
                } else if (dirtyObject instanceof Trade) {
                    save(commandOffset, (Trade) dirtyObject);
                } else if (dirtyObject instanceof OrderLog) {
                    save(commandOffset, (OrderLog) dirtyObject);
                }
            }
        });
    }

    private void save(Long commandOffset, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrSavedCount(commandOffset);
                    });
        });
    }

    private void save(Long commandOffset, Order order) {
        kafkaExecutor.execute(order.getUserId(), () -> {
            String data = JSON.toJSONString(order);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), order.getUserId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrSavedCount(commandOffset);
                    });
        });

        simpleOrderBookExecutor.execute(order.getProductId(), () -> {
            SimpleOrderBook orderBook = simpleOrderBooks.computeIfAbsent(order.getProductId(),
                    k -> new SimpleOrderBook(order.getProductId()));
            if (order.getStatus() == OrderStatus.OPEN) {
                //orderBook.putOrder(order);
            } else {
                //orderBook.removeOrder(order);
            }
        });
    }

    private void save(Long commandOffset, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrSavedCount(commandOffset);
                    });
            //tradeTopic.publishAsync(data);
        });
    }

    private void save(Long commandOffset, OrderLog orderLog) {
        decrSavedCount(commandOffset);
        String productId = orderLog.getProductId();

        redisExecutor.execute(orderLog.getProductId(), () -> {
            String data = JSON.toJSONString(orderLog);
            //orderBookTopic.publishAsync(data);
        });

        simpleOrderBookExecutor.execute(orderLog.getProductId(), () -> {
            //SimpleOrderBook orderBook = simpleOrderBooks.computeIfAbsent(productId,
            //    k -> new SimpleOrderBook(productId));
            //orderBook.setSequence(orderLog.getSequence());
            //simpleOrderBooks.get(productId).setSequence(orderLog.getSequence());
        });
    }

    private void save(Long commandOffset, Object notify) {

    }

    private void decrSavedCount(Long commandOffset) {
        ModifiedObjectList<Object> dirtyObjects = modifiedObjectsByCommandOffset.get(commandOffset);
        if (dirtyObjects.getSavedCount().incrementAndGet() == dirtyObjects.size()) {
            //logger.info("all flushed: commandOffset={}, size={}", commandOffset, dirtyObjects.size());
        }
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

        var itr = modifiedObjectsByCommandOffset.entrySet().iterator();
        while (itr.hasNext()) {
            var entry = itr.next();
            ModifiedObjectList<Object> dirtyObjects = entry.getValue();
            if (!dirtyObjects.isAllSaved()) {
                break;
            }

            commandOffset = entry.getKey();

            for (Object obj : dirtyObjects) {
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
                }
            }

            itr.remove();
        }

        if (commandOffset == null) {
            return;
        }
        matchingEngineStateStore.write(commandOffset, accounts, orders, products, tradeIdByProductId,
                sequenceByProductId);
    }

    private void saveOrderBook() {
        simpleOrderBooks.forEach((k, v) -> {
            L2OrderBook l2OrderBook = new L2OrderBook(v);
            logger.info(JSON.toJSONString(l2OrderBook));
        });
    }
}
