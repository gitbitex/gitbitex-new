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
    private final LogWriter logWriter;
    private final AtomicLong logSequence = new AtomicLong();
    private Long commandOffset;

    public MatchingEngine(MatchingEngineStateStore matchingEngineStateStore, LogWriter logWriter) {
        this.logWriter = logWriter;
        this.accountBook=new AccountBook(logWriter);

        this.commandOffset = matchingEngineStateStore.getCommandOffset();
        if (this.commandOffset == null) {
            Map<String, Long> tradeIds = matchingEngineStateStore.getTradeIds();
            Map<String, Long> sequences = matchingEngineStateStore.getSequences();

            matchingEngineStateStore.forEachAccount(accountBook::add);
            matchingEngineStateStore.forEachOrder(order -> orderBooks
                .computeIfAbsent(order.getProductId(), k -> new OrderBook(order.getProductId(), null,
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
        commandOffset = command.getOffset();
        OrderBook orderBook = createOrderBook(command.getProductId());
        orderBook.placeOrder(new Order(command), commandOffset);
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, logWriter, null, null, accountBook, productBook);
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
}
