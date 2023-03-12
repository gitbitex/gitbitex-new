package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook = new AccountBook();
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final EngineSnapshotStore stateStore;
    private final Counter commandProcessedCounter;
    List<EngineListener> engineListeners;
    @Getter
    private Long startupCommandOffset;

    public MatchingEngine(EngineSnapshotStore stateStore, List<EngineListener> engineListeners) {
        this.stateStore = stateStore;
        this.engineListeners = engineListeners;
        this.commandProcessedCounter = Counter.builder("gbe.matching-engine.command.processed")
                .register(Metrics.globalRegistry);
        restoreState();
        //startModifiedObjectSaveTask();
    }

    public void shutdown() {
        scheduledExecutor.shutdown();
    }

    public void executeCommand(DepositCommand command) {
        commandProcessedCounter.increment();
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), null);
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId(), modifiedObjects);
        //engineListener.onCommandExecuted(modifiedObjects);
        engineListeners.forEach(x -> x.onCommandExecuted(command, modifiedObjects));
    }

    public void executeCommand(PutProductCommand command) {
        commandProcessedCounter.increment();
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), null);
        productBook.putProduct(new Product(command), modifiedObjects);
        createOrderBook(command.getProductId());
        //engineListener.onCommandExecuted(modifiedObjects);
        engineListeners.forEach(x -> x.onCommandExecuted(command, modifiedObjects));
    }

    public void executeCommand(PlaceOrderCommand command) {
        commandProcessedCounter.increment();
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), command.getProductId());
        orderBook.placeOrder(new Order(command), modifiedObjects);
        //engineListener.onCommandExecuted(modifiedObjects);
        engineListeners.forEach(x -> x.onCommandExecuted(command, modifiedObjects));
    }

    public void executeCommand(CancelOrderCommand command) {
        commandProcessedCounter.increment();
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }
        ModifiedObjectList modifiedObjects = new ModifiedObjectList(command.getOffset(), command.getProductId());
        orderBook.cancelOrder(command.getOrderId(), modifiedObjects);
        //engineListener.onCommandExecuted(modifiedObjects);
        engineListeners.forEach(x -> x.onCommandExecuted(command, modifiedObjects));
    }

    private void createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook != null) {
            return;
        }
        orderBook = new OrderBook(productId, null, null, null, accountBook, productBook);
        orderBooks.put(productId, orderBook);
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

   /*
   private void dispatch(ModifiedObjectList modifiedObjects) {
        modifiedObjectWriter.saveAsync(modifiedObjects);
        orderBookSnapshotTaker.refresh(modifiedObjects);
        engineSnapshotTaker.append(modifiedObjects);
    }

    private void startModifiedObjectSaveTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (true) {
                ModifiedObjectList modifiedObjects = modifiedObjectListQueue.poll();
                if (modifiedObjects == null) {
                    break;
                }
                dispatch(modifiedObjects);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
*/

}
