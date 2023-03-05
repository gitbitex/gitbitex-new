package com.gitbitex.matchingengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.L3OrderBook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook;
    @Getter
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final DirtyObjectHandler dirtyObjectHandler;
    private final AtomicLong logSequence = new AtomicLong();
    private Long commandOffset;

    public MatchingEngine(MatchingEngineStateStore matchingEngineStateStore, DirtyObjectHandler dirtyObjectHandler) {
        this.dirtyObjectHandler = dirtyObjectHandler;
        this.accountBook=new AccountBook(dirtyObjectHandler);

        this.commandOffset = matchingEngineStateStore.getCommandOffset();
        if (this.commandOffset == null) {
            Map<String, Long> tradeIds = matchingEngineStateStore.getTradeIds();
            Map<String, Long> sequences = matchingEngineStateStore.getSequences();

            matchingEngineStateStore.forEachAccount(accountBook::add);
            matchingEngineStateStore.forEachOrder(order -> orderBooks
                .computeIfAbsent(order.getProductId(), k -> new OrderBook(order.getProductId(),
                    tradeIds.get(order.getProductId()), sequences.get(order.getProductId()), accountBook, productBook))
                .addOrder(order));
        }
    }

    public void executeCommand(DepositCommand command) {
        commandOffset = command.getOffset();
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
            command.getTransactionId(), commandOffset);
    }

    public void executeCommand(PlaceOrderCommand command) {
        //commandOffset = command.getOffset();
        OrderBook orderBook = createOrderBook(command.getProductId());
        DirtyObjectList<Object> dirtyObjects= orderBook.placeOrder(new Order(command));
        flush(commandOffset,dirtyObjects);
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, null, null, accountBook, productBook);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    public void executeCommand(CancelOrderCommand command) {
        commandOffset = command.getOffset();
        orderBooks.get(command.getProductId()).cancelOrder(command.getOrderId(), commandOffset);
    }

    public MatchingEngineSnapshot takeSnapshot() {
        List<Product> products = this.productBook.getProducts().stream()
            .map(Product::clone)
            .collect(Collectors.toList());
        Set<Account> accounts = this.accountBook.getAllAccounts().stream()
            .map(Account::clone)
            .collect(Collectors.toSet());
        List<OrderBookSnapshot> orderBookSnapshots = this.orderBooks.values().stream()
            .map(OrderBookSnapshot::new)
            .collect(Collectors.toList());
        MatchingEngineSnapshot snapshot = new MatchingEngineSnapshot();
        snapshot.setProducts(products);
        snapshot.setAccounts(accounts);
        snapshot.setOrderBookSnapshots(orderBookSnapshots);
        snapshot.setLogSequence(logSequence.get());
        snapshot.setCommandOffset(commandOffset);
        snapshot.setTime(System.currentTimeMillis());
        return snapshot;
    }

    public L2OrderBook takeL2OrderBookSnapshot(String productId, int depth) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            return null;
        }
        return new L2OrderBook(orderBook, depth);
    }

    public L3OrderBook takeL3OrderBookSnapshot(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            return null;
        }
        return new L3OrderBook(orderBook);
    }

    private void flush(Long commandOffset, DirtyObjectList<Object> dirtyObjects) {
        if (dirtyObjectHandler != null) {
            for (int i = 0; i < dirtyObjects.size(); i++) {
                Object obj = dirtyObjects.get(i);
                if (obj instanceof Order) {
                    dirtyObjects.set(i, ((Order)obj).clone());
                } else if (obj instanceof Account) {
                    dirtyObjects.set(i, ((Account)obj).clone());
                }
            }
            dirtyObjectHandler.flush(commandOffset, dirtyObjects);
        }
    }
}
