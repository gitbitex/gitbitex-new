package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderStatus;
import com.gitbitex.matchingengine.command.Command;
import com.gitbitex.stripexecutor.StripedExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Component
@Slf4j
public class OrderBookSnapshotTaker implements EngineListener {
    private final OrderBookSnapshotStore orderBookSnapshotStore;
    private final EngineSnapshotStore engineSnapshotStore;
    private final ConcurrentHashMap<String, SimpleOrderBook> simpleOrderBooks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastL2OrderBookSequences = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ModifiedObjectList> modifiedObjectsQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private final StripedExecutorService orderBookSnapshotExecutor =
            new StripedExecutorService(Runtime.getRuntime().availableProcessors(),
                    new ThreadFactoryBuilder().setNameFormat("OrderBookSnapshot-%s").build());
    private long lastCommandOffset;

    public OrderBookSnapshotTaker(EngineSnapshotStore engineSnapshotStore,
                                  OrderBookSnapshotStore orderBookSnapshotStore) {
        this.engineSnapshotStore = engineSnapshotStore;
        this.orderBookSnapshotStore = orderBookSnapshotStore;
        startMainTask();
        startL2OrderBookPublishTask();
        restoreState();
    }

    @PreDestroy
    public void close() {
        scheduledExecutor.shutdown();
        orderBookSnapshotExecutor.shutdown();
    }

    @Override
    public void onCommandExecuted(Command command, ModifiedObjectList modifiedObjects) {
        if (lastCommandOffset != 0 && modifiedObjects.getCommandOffset() <= lastCommandOffset) {
            logger.info("received processed message: {}", modifiedObjects.getCommandOffset());
            return;
        }
        lastCommandOffset = modifiedObjects.getCommandOffset();
        modifiedObjectsQueue.offer(modifiedObjects);
    }

    private void updateOrderBook(ModifiedObjectList modifiedObjects) {
        String productId = modifiedObjects.getProductId();
        if (productId == null) {
            return;
        }
        orderBookSnapshotExecutor.execute(productId, () -> {
            SimpleOrderBook simpleOrderBook = getOrderBook(productId);
            modifiedObjects.forEach(obj -> {
                if (obj instanceof Order order) {
                    if (order.getStatus() == OrderStatus.OPEN) {
                        simpleOrderBook.addOrder(order);
                    } else {
                        simpleOrderBook.removeOrder(order);
                    }
                } else if (obj instanceof OrderBookState orderBookState) {
                    simpleOrderBook.setMessageSequence(orderBookState.getMessageSequence());
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

    private SimpleOrderBook getOrderBook(String productId) {
        SimpleOrderBook simpleOrderBook = simpleOrderBooks.get(productId);
        if (simpleOrderBook == null) {
            simpleOrderBook = new SimpleOrderBook(productId);
            simpleOrderBooks.put(productId, simpleOrderBook);
        }
        return simpleOrderBook;
    }

    private void startMainTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (true) {
                ModifiedObjectList modifiedObjects = modifiedObjectsQueue.poll();
                if (modifiedObjects == null) {
                    break;
                }
                updateOrderBook(modifiedObjects);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void startL2OrderBookPublishTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            simpleOrderBooks.forEach((productId, simpleOrderBook) -> {
                orderBookSnapshotExecutor.execute(productId, () -> takeL2OrderBookSnapshot(simpleOrderBook, 0));
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
