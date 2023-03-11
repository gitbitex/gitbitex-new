package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderStatus;
import com.gitbitex.stripexecutor.StripedExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class OrderBookSnapshotPublisher {
    private final OrderBookSnapshotStore orderBookSnapshotStore;
    private final ConcurrentHashMap<String, SimpleOrderBook> simpleOrderBooks = new ConcurrentHashMap<>();
    private final StripedExecutorService orderBookSnapshotExecutor =
            new StripedExecutorService(Runtime.getRuntime().availableProcessors(),
                    new ThreadFactoryBuilder().setNameFormat("OrderBookSnapshot-%s").build());
    private final ConcurrentHashMap<String, Long> lastL2OrderBookSequences = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);
    private final EngineSnapshotStore engineSnapshotStore;

    public OrderBookSnapshotPublisher(EngineSnapshotStore engineSnapshotStore, OrderBookSnapshotStore orderBookSnapshotStore) {
        this.engineSnapshotStore = engineSnapshotStore;
        this.orderBookSnapshotStore = orderBookSnapshotStore;
        startL2OrderBookPublishTask();
        restoreState();
    }

    public void refresh(ModifiedObjectList modifiedObjects) {
        String productId = modifiedObjects.getProductId();
        if (productId == null) {
            return;
        }
        orderBookSnapshotExecutor.execute(productId, () -> {
            simpleOrderBooks.putIfAbsent(productId, new SimpleOrderBook(productId));
            SimpleOrderBook simpleOrderBook = simpleOrderBooks.get(productId);
            modifiedObjects.forEach(obj -> {
                if (obj instanceof Order) {
                    Order order = (Order) obj;
                    if (order.getStatus() == OrderStatus.OPEN) {
                        simpleOrderBook.addOrder(order);
                    } else {
                        simpleOrderBook.removeOrder(order);
                    }
                } else if (obj instanceof OrderBookState) {
                    OrderBookState orderBookState = (OrderBookState) obj;
                    simpleOrderBook.setMessageSequence(orderBookState.getMessageSequence());
                } else if (obj instanceof OrderBookCompleteNotify) {
                    takeL2OrderBookSnapshot(simpleOrderBook, 200);
                }
            });
        });
    }

    private void takeL2OrderBookSnapshot(SimpleOrderBook simpleOrderBook, long delta) {
        String productId = simpleOrderBook.getProductId();
        long lastL2OrderBookSequence = lastL2OrderBookSequences.getOrDefault(productId, 0L);
        if (simpleOrderBook.getMessageSequence() - lastL2OrderBookSequence > delta) {
            L2OrderBook l2OrderBook = new L2OrderBook(simpleOrderBook, 25);
            lastL2OrderBookSequences.put(productId, simpleOrderBook.getMessageSequence());
            orderBookSnapshotStore.saveL2BatchOrderBook(l2OrderBook);
        }
    }

    public void startL2OrderBookPublishTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            simpleOrderBooks.forEach((productId, simpleOrderBook) -> {
                orderBookSnapshotExecutor.execute(productId, () -> {
                    takeL2OrderBookSnapshot(simpleOrderBook, 0);
                });
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void restoreState() {
        if (engineSnapshotStore.getCommandOffset() == null) {
            return;
        }
        engineSnapshotStore.getOrderBookStates().forEach(x -> {
            SimpleOrderBook simpleOrderBook = new SimpleOrderBook(x.getProductId(), x.getMessageSequence());
            simpleOrderBooks.put(x.getProductId(), simpleOrderBook);
            engineSnapshotStore.getOrders(x.getProductId()).forEach(simpleOrderBook::addOrder);
        });
    }
}
