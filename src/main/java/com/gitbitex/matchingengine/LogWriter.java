package com.gitbitex.matchingengine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderLog;
import com.gitbitex.matchingengine.log.OrderMatchMessage;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.stripexecutor.StripedExecutorService;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.RocksDB;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogWriter {
    static {
        RocksDB.loadLibrary();
    }

    private final KafkaMessageProducer producer;
    private final RedissonClient redissonClient;
    private final AppProperties appProperties;
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic tradeTopic;
    private final RTopic orderBookTopic;
    private final MatchingEngineStateStore matchingEngineStateStore;// = new MatchingEngineStateStore();
    BlockingQueue<Runnable> orderQueue = new LinkedBlockingQueue<>(10000000);
    ThreadPoolExecutor mainExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, orderQueue);
    StripedExecutorService kafkaExecutor = new StripedExecutorService(2);
    StripedExecutorService redisExecutor = new StripedExecutorService(2);
    StripedExecutorService l2OrderBookExecutor = new StripedExecutorService(2);
    ScheduledExecutorService snapshotExecutor = Executors.newScheduledThreadPool(1);
    ConcurrentSkipListMap<Long, DirtyObjectList<Object>> dirtyObjectsByCommandOffset = new ConcurrentSkipListMap<>(
        Comparator.naturalOrder());
    OptimisticTransactionDB db;
    ConcurrentHashMap<String, SimpleOrderBook> orderBookByProductId = new ConcurrentHashMap<>();

    public LogWriter(KafkaMessageProducer producer, RedissonClient redissonClient,
        MatchingEngineStateStore matchingEngineStateStore, AppProperties appProperties) {
        this.producer = producer;
        this.redissonClient = redissonClient;
        this.appProperties = appProperties;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.tradeTopic = redissonClient.getTopic("trade", StringCodec.INSTANCE);
        this.orderBookTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
        this.matchingEngineStateStore = matchingEngineStateStore;
        snapshotExecutor.scheduleWithFixedDelay(() -> {
            try {
                createSnapshot();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);

        snapshotExecutor.scheduleWithFixedDelay(() -> {
            try {
                orderBookByProductId.forEach((k,v)->{
                    l2OrderBookExecutor.execute(k,()->{
                        L2OrderBook l2OrderBook=new L2OrderBook(v);
                        logger.info("l2OrderBook {}",JSON.toJSONString(l2OrderBook,true));
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.SECONDS);

    }

    @SneakyThrows
    private void createSnapshot() {
        long beginCommandOffset;
        long endCommandOffset;
        Set<Account> accounts = new HashSet<>();
        Set<Order> orders = new HashSet<>();
        Set<Product> products = new HashSet<>();
        Map<String, Long> tradeIdByProductId = new HashMap<>();
        Map<String, Long> sequenceByProductId = new HashMap<>();

        if (dirtyObjectsByCommandOffset.isEmpty()) {
            return;
        }

        int i = 0;
        var itr = dirtyObjectsByCommandOffset.entrySet().iterator();
        beginCommandOffset = endCommandOffset = dirtyObjectsByCommandOffset.firstKey();
        while (itr.hasNext()) {
            var entry = itr.next();
            DirtyObjectList<Object> dirtyObjects = entry.getValue();
            if (dirtyObjects.isAllFlushed()) {
                for (Object obj : dirtyObjects) {
                    if (obj instanceof Account) {
                        accounts.add((Account)obj);
                    } else if (obj instanceof Order) {
                        orders.add((Order)obj);
                    } else if (obj instanceof Trade) {
                        Trade trade = (Trade)obj;
                        tradeIdByProductId.put(trade.getProductId(), trade.getTradeId());
                    } else if (obj instanceof OrderLog) {
                        OrderLog orderLog = (OrderLog)obj;
                        sequenceByProductId.put(orderLog.getProductId(), orderLog.getSequence());
                    }
                    endCommandOffset = entry.getKey();
                }
                itr.remove();
                if (endCommandOffset - beginCommandOffset >= 2222) {
                    break;
                }
            } else {
                break;
            }
        }

        matchingEngineStateStore.write(endCommandOffset, accounts, orders, products, tradeIdByProductId,
            sequenceByProductId);

        System.out.println(matchingEngineStateStore.getCommandOffset());
        System.out.println("order");
        matchingEngineStateStore.forEachOrder(x -> {
            System.out.println(JSON.toJSONString(x));
        });
        System.out.println("account");
        matchingEngineStateStore.forEachAccount(x -> {
            System.out.println(JSON.toJSONString(x));
        });
        System.out.println("tradeid");
        matchingEngineStateStore.forEachTradeId((productId, tradeId) -> {
            System.out.println(productId + "=" + tradeId);
        });
        System.out.println("sequence");
        matchingEngineStateStore.forEachSequence((productId, sequence) -> {
            System.out.println(productId + "=" + sequence);
        });

    }

    public void flush(Long commandOffset, DirtyObjectList<Object> dirtyObjects) {
        dirtyObjectsByCommandOffset.put(commandOffset, dirtyObjects);
        for (Object dirtyObject : dirtyObjects) {
            if (dirtyObject instanceof Order) {
                flush(commandOffset, (Order)dirtyObject);
            } else if (dirtyObject instanceof Account) {
                flush(commandOffset, (Account)dirtyObject);
            } else if (dirtyObject instanceof Trade) {
                flush(commandOffset, (Trade)dirtyObject);
            } else if (dirtyObject instanceof OrderLog) {
                flush(commandOffset, (OrderLog)dirtyObject);
                decrRefCount(commandOffset);
            }
        }
    }

    public void flush(Long commandOffset, Account account) {
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

    public void flush(Long commandOffset, Order order) {
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
            SimpleOrderBook orderBook = orderBookByProductId.computeIfAbsent(order.getProductId(),
                k -> new SimpleOrderBook());
            if (order.getStatus() == OrderStatus.OPEN) {
                orderBook.putOrder(order);
            } else {
                orderBook.removeOrder(order);
            }

        });
    }

    public void flush(Long commandOffset, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data),
                (recordMetadata, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    }
                    decrRefCount(commandOffset);
                });
        });
    }

    public void flush(Long commandOffset, OrderLog orderLog) {
        redisExecutor.execute(orderLog.getProductId(), () -> {
            String data = JSON.toJSONString(orderLog);
            orderBookTopic.publishAsync(data);
        });
    }

    private void decrRefCount(Long commandOffset) {
        DirtyObjectList<Object> dirtyObjects = dirtyObjectsByCommandOffset.get(commandOffset);
        if (dirtyObjects.getFlushedCount().incrementAndGet() == dirtyObjects.size()) {
            logger.info("all flushed: commandOffset={}, size={}", commandOffset, dirtyObjects.size());
            //createSafePoint(commandOffset);
        } else {
            //logger.info("not safe");
        }
    }

    public void onOrderReceived(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderReceivedMessage message = new OrderReceivedMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            message.setUserId(order.getUserId());
            message.setPrice(order.getPrice());
            message.setFunds(order.getRemainingFunds());
            message.setSide(order.getSide());
            message.setSize(order.getRemainingSize());
            message.setOrderId(order.getOrderId());
            message.setOrderType(order.getType());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderOpen(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderOpenMessage message = new OrderOpenMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            message.setRemainingSize(order.getRemainingSize());
            message.setPrice(order.getPrice());
            message.setSide(order.getSide());
            message.setOrderId(order.getOrderId());
            message.setUserId(order.getUserId());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderMatch(Order takerOrder, Order makerOrder, Trade trade, long sequence) {
        mainExecutor.execute(() -> {
            String data = JSON.toJSONString(trade);
            producer.send(new ProducerRecord<>(appProperties.getTradeMessageTopic(), trade.getProductId(), data));
            tradeTopic.publishAsync(data);

            OrderMatchMessage message = new OrderMatchMessage();
            message.setSequence(sequence);
            message.setTradeId(trade.getTradeId());
            message.setProductId(trade.getProductId());
            message.setTakerOrderId(takerOrder.getOrderId());
            message.setMakerOrderId(makerOrder.getOrderId());
            message.setTakerUserId(takerOrder.getUserId());
            message.setMakerUserId(makerOrder.getUserId());
            message.setPrice(makerOrder.getPrice());
            message.setSize(trade.getSize());
            message.setFunds(trade.getFunds());
            message.setSide(makerOrder.getSide());
            message.setTime(takerOrder.getTime());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public void onOrderDone(Order order, long sequence) {
        mainExecutor.execute(() -> {
            OrderDoneMessage message = new OrderDoneMessage();
            message.setSequence(sequence);
            message.setProductId(order.getProductId());
            if (order.getType() != OrderType.MARKET) {
                message.setRemainingSize(order.getRemainingSize());
                message.setPrice(order.getPrice());
            }
            message.setRemainingFunds(order.getRemainingFunds());
            message.setRemainingSize(order.getRemainingSize());
            message.setSide(order.getSide());
            message.setOrderId(order.getOrderId());
            message.setUserId(order.getUserId());
            //log.setDoneReason(doneReason);
            message.setOrderType(order.getType());
            message.setTime(new Date());
            orderBookTopic.publishAsync(JSON.toJSONString(message));
        });
    }

    public static class DirtyObjectList<T> extends ArrayList<T> {
        @Getter
        private final AtomicLong flushedCount = new AtomicLong();

        public static <T> DirtyObjectList<T> singletonList(T obj) {
            DirtyObjectList<T> list = new DirtyObjectList<>();
            list.add(obj);
            return list;
        }

        public boolean isAllFlushed() {
            return flushedCount.get() == size();
        }
    }

}
