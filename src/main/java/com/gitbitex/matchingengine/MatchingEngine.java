package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook = new AccountBook();
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final EngineStateStore stateStore;
    private final ConcurrentLinkedQueue<ModifiedObjectList> modifiedObjectsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong modifiedObjectsQueueSizeCounter = new AtomicLong();
    private final Counter commandProcessedCounter;
    private final ModifiedObjectWriter modifiedObjectWriter;
    private final EngineStateWriter engineStateWriter;
    private final OrderBookSnapshotPublisher orderBookSnapshotPublisher;
    @Getter
    private Long startupCommandOffset;

    public MatchingEngine(EngineStateStore stateStore, ModifiedObjectWriter modifiedObjectWriter,
                          EngineStateWriter engineStateWriter, OrderBookSnapshotPublisher orderBookSnapshotPublisher) {
        this.stateStore = stateStore;
        this.modifiedObjectWriter = modifiedObjectWriter;
        this.engineStateWriter = engineStateWriter;
        this.orderBookSnapshotPublisher = orderBookSnapshotPublisher;
        this.commandProcessedCounter = Counter.builder("gbe.matching-engine.command.processed")
                .register(Metrics.globalRegistry);
        Gauge.builder("gbe.matching-engine.modified-objects-queue.size", modifiedObjectsQueueSizeCounter::get)
                .register(Metrics.globalRegistry);
        restoreState();
        startModifiedObjectSaveTask();
    }

    public void shutdown() {
        scheduledExecutor.shutdown();
    }

    public void executeCommand(DepositCommand command) {
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), null);
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId(), modifiedObjects);
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
            orderBook = new OrderBook(productId, null, null, null, accountBook, productBook);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    private void enqueue(ModifiedObjectList modifiedObjects) {
        while (modifiedObjectsQueueSizeCounter.get() >= 1000000) {
            logger.warn("modified objects queue is full");
            Thread.yield();
        }
        modifiedObjectsQueue.offer(modifiedObjects);
        modifiedObjectsQueueSizeCounter.incrementAndGet();
    }

    private void dispatch(ModifiedObjectList modifiedObjects) {
        modifiedObjectWriter.saveAsync(modifiedObjects);
        orderBookSnapshotPublisher.refresh(modifiedObjects);
        engineStateWriter.append(modifiedObjects);
    }

    private void restoreState() {
        startupCommandOffset = stateStore.getCommandOffset();
        if (startupCommandOffset == null) {
            return;
        }
        stateStore.getAccounts().forEach(accountBook::add);
        stateStore.getProducts().forEach(productBook::addProduct);
        stateStore.getOrderBookStates().forEach(x -> {
            OrderBook orderBook = new OrderBook(x.getProductId(), x.getOrderSequence(), x.getTradeSequence(),
                    x.getMessageSequence(), accountBook, productBook);
            orderBooks.put(x.getProductId(), orderBook);
            stateStore.getOrders(x.getProductId()).forEach(orderBook::addOrder);
        });
    }

    private void startModifiedObjectSaveTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (true) {
                ModifiedObjectList modifiedObjects = modifiedObjectsQueue.poll();
                if (modifiedObjects == null) {
                    break;
                }
                modifiedObjectsQueueSizeCounter.decrementAndGet();
                dispatch(modifiedObjects);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }


}
