package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.log.OrderLog;
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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook;
    @Getter
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final Map<String, SimpleOrderBook> simpleOrderBooks = new HashMap<>();
    private final StripedExecutorService simpleOrderBookExecutor = new StripedExecutorService(2);
    //private final DirtyObjectHandler dirtyObjectHandler;
    private final ConcurrentSkipListMap<Long, DirtyObjectList<Object>> dirtyObjectsByCommandOffset = new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService snapshotExecutor = Executors.newScheduledThreadPool(1);
    private final StripedExecutorService kafkaExecutor = new StripedExecutorService(2);
    private final StripedExecutorService redisExecutor = new StripedExecutorService(2);
    KafkaMessageProducer producer;
    RedissonClient redissonClient;
    AppProperties appProperties;
    MatchingEngineStateStore matchingEngineStateStore;

    public MatchingEngine(MatchingEngineStateStore matchingEngineStateStore, KafkaMessageProducer producer, RedissonClient redissonClient, AppProperties appProperties) {
        //this.dirtyObjectHandler = dirtyObjectHandler;
        this.accountBook = new AccountBook();
        this.appProperties=appProperties;
        this.producer=producer;
        this.redissonClient=redissonClient;
        this.matchingEngineStateStore=matchingEngineStateStore;

        Long commandOffset = matchingEngineStateStore.getCommandOffset();
        if (commandOffset == null) {
            Map<String, Long> tradeIds = matchingEngineStateStore.getTradeIds();
            Map<String, Long> sequences = matchingEngineStateStore.getSequences();

            matchingEngineStateStore.forEachAccount(accountBook::add);
            matchingEngineStateStore.forEachOrder(order -> {
                String productId = order.getProductId();
                orderBooks.computeIfAbsent(productId, k -> new OrderBook(productId, tradeIds.get(productId), sequences.get(productId), accountBook, productBook))
                        .addOrder(order);
                simpleOrderBooks.computeIfAbsent(productId, k -> new SimpleOrderBook(productId, sequences.get(productId)))
                        .putOrder(order);
            });
        }

        snapshotExecutor.scheduleAtFixedRate(() -> {
            try {
                saveMatchingEngineState();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void executeCommand(DepositCommand command) {
        DirtyObjectList<Object> dirtyObjects = accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId(), command.getOffset());
        flush(command.getOffset(), dirtyObjects);
    }

    public void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = createOrderBook(command.getProductId());
        DirtyObjectList<Object> dirtyObjects = orderBook.placeOrder(new Order(command));
        flush(command.getOffset(), dirtyObjects);
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

    /*private void flushk(Long commandOffset, DirtyObjectList<Object> dirtyObjects) {
        if (dirtyObjectHandler != null) {
            for (int i = 0; i < dirtyObjects.size(); i++) {
                Object obj = dirtyObjects.get(i);
                if (obj instanceof Order) {
                    dirtyObjects.set(i, ((Order) obj).clone());
                } else if (obj instanceof Account) {
                    dirtyObjects.set(i, ((Account) obj).clone());
                }
            }
            dirtyObjectHandler.flush(commandOffset, dirtyObjects);
        }
    }*/

    private void flush(Long commandOffset, DirtyObjectList<Object> dirtyObjects) {
        dirtyObjectsByCommandOffset.put(commandOffset, dirtyObjects);
        for (Object dirtyObject : dirtyObjects) {
            if (dirtyObject instanceof Order) {
                flush(commandOffset, (Order) dirtyObject);
            } else if (dirtyObject instanceof Account) {
                flush(commandOffset, (Account) dirtyObject);
            } else if (dirtyObject instanceof Trade) {
                flush(commandOffset, (Trade) dirtyObject);
            } else if (dirtyObject instanceof OrderLog) {
                flush(commandOffset, (OrderLog) dirtyObject);
            }
        }
    }

    private void flush(Long commandOffset, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.send(new ProducerRecord<>(appProperties.getAccountMessageTopic(), account.getUserId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrRefCount(commandOffset);
                    });
        });
    }

    private void flush(Long commandOffset, Order order) {
        kafkaExecutor.execute(order.getUserId(), () -> {
            String data = JSON.toJSONString(order);
            producer.send(new ProducerRecord<>(appProperties.getOrderMessageTopic(), order.getUserId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrRefCount(commandOffset);
                    });
        });

        simpleOrderBookExecutor.execute(order.getProductId(), () -> {
            SimpleOrderBook orderBook = simpleOrderBooks.computeIfAbsent(order.getProductId(), k -> new SimpleOrderBook(order.getProductId()));
            if (order.getStatus() == OrderStatus.OPEN) {
                orderBook.putOrder(order);
            } else {
                orderBook.removeOrder(order);
            }
        });
    }

    private void flush(Long commandOffset, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data),
                    (recordMetadata, e) -> {
                        if (e != null) {
                            throw new RuntimeException(e);
                        }
                        decrRefCount(commandOffset);
                    });
            //tradeTopic.publishAsync(data);
        });
    }

    private void flush(Long commandOffset, OrderLog orderLog) {
        decrRefCount(commandOffset);

        redisExecutor.execute(orderLog.getProductId(), () -> {
            String data = JSON.toJSONString(orderLog);
            //orderBookTopic.publishAsync(data);
        });

        simpleOrderBookExecutor.execute(orderLog.getProductId(), () -> {
            SimpleOrderBook orderBook = simpleOrderBooks.computeIfAbsent(orderLog.getProductId(), k -> new SimpleOrderBook(orderLog.getProductId()));
            orderBook.setSequence(orderLog.getSequence());
        });
    }

    private void flush(Object notify) {

    }

    private void decrRefCount(Long commandOffset) {
        DirtyObjectList<Object> dirtyObjects = dirtyObjectsByCommandOffset.get(commandOffset);
        if (dirtyObjects.getFlushedCount().incrementAndGet() == dirtyObjects.size()) {
            logger.info("all flushed: commandOffset={}, size={}", commandOffset, dirtyObjects.size());
        }
    }

    private void saveMatchingEngineState() {
        if (dirtyObjectsByCommandOffset.isEmpty()) {
            return;
        }

        Long commandOffset = null;
        Set<Account> accounts = new HashSet<>();
        Set<Order> orders = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Map<String, Long> tradeIdByProductId = new HashMap<>();
        Map<String, Long> sequenceByProductId = new HashMap<>();

        var itr = dirtyObjectsByCommandOffset.entrySet().iterator();
        while (itr.hasNext()) {
            var entry = itr.next();
            DirtyObjectList<Object> dirtyObjects = entry.getValue();
            if (!dirtyObjects.isAllFlushed()) {
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
}
