package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.OrderLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class DirtyObjectHandler {
    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic tradeTopic;
    private final RTopic orderBookTopic;
    private final OrderBookManager orderBookManager;
    private final MatchingEngineStateStore matchingEngineStateStore;// = new MatchingEngineStateStore();
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    ThreadPoolExecutor mainExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService kafkaExecutor = new StripedExecutorService(2);
    StripedExecutorService redisExecutor = new StripedExecutorService(2);
    StripedExecutorService l2OrderBookExecutor = new StripedExecutorService(2);
    ScheduledExecutorService snapshotExecutor = Executors.newScheduledThreadPool(1);
    private final ConcurrentSkipListMap<Long, DirtyObjectList<Object>> dirtyObjectsByCommandOffset = new ConcurrentSkipListMap<>(
            Comparator.naturalOrder());
    ConcurrentHashMap<String, SimpleOrderBook> orderBookByProductId = new ConcurrentHashMap<>();

    public DirtyObjectHandler(KafkaMessageProducer producer, RedissonClient redissonClient, OrderBookManager orderBookManager,
                              MatchingEngineStateStore matchingEngineStateStore, AppProperties appProperties) {
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.appProperties = appProperties;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
        this.orderBookTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
        this.matchingEngineStateStore = matchingEngineStateStore;
        this.orderBookManager = orderBookManager;
        snapshotExecutor.scheduleAtFixedRate(() -> {
            try {
                saveMatchingEngineState();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);

        snapshotExecutor.scheduleAtFixedRate(() -> {
            try {
                orderBookByProductId.forEach((k, v) -> {
                    l2OrderBookExecutor.execute(k, () -> {
                        L2OrderBook l2OrderBook = new L2OrderBook(v);
                        //logger.info("l2OrderBook {}", JSON.toJSONString(l2OrderBook, true));
                        orderBookManager.saveL2BatchOrderBook(l2OrderBook);
                        //redissonClient.getTopic("l2_batch", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(l2OrderBook));
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);

        matchingEngineStateStore.forEachOrder(x->{
            orderBookByProductId.computeIfAbsent(x.getProductId(),k->new SimpleOrderBook(x.getProductId())).putOrder(x);
        });


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

    public void flush(Long commandOffset, DirtyObjectList<Object> dirtyObjects) {
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

        l2OrderBookExecutor.execute(order.getProductId(), () -> {
            SimpleOrderBook orderBook = orderBookByProductId.computeIfAbsent(order.getProductId(), k -> new SimpleOrderBook(order.getProductId()));
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
            tradeTopic.publishAsync(data);
        });
    }

    private void flush(Long commandOffset,OrderLog orderLog) {
        decrRefCount(commandOffset);

        redisExecutor.execute(orderLog.getProductId(), () -> {
            String data = JSON.toJSONString(orderLog);
            orderBookTopic.publishAsync(data);
        });

        l2OrderBookExecutor.execute(orderLog.getProductId(), () -> {
            SimpleOrderBook orderBook = orderBookByProductId.computeIfAbsent(orderLog.getProductId(), k -> new SimpleOrderBook(orderLog.getProductId()));
            orderBook.setSequence(orderLog.getSequence());
        });
    }

    private void flush(Object notify){

    }

    private void decrRefCount(Long commandOffset) {
        DirtyObjectList<Object> dirtyObjects = dirtyObjectsByCommandOffset.get(commandOffset);
        if (dirtyObjects.getFlushedCount().incrementAndGet() == dirtyObjects.size()) {
            logger.info("all flushed: commandOffset={}, size={}", commandOffset, dirtyObjects.size());
        }
    }

}
