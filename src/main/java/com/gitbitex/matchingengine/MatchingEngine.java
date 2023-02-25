package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.L3OrderBook;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class MatchingEngine {
    private final ProductBook productBook = new ProductBook();
    private final AccountBook accountBook;
    @Getter
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final LogWriter logWriter;
    private final AtomicLong logSequence = new AtomicLong();
    private long commandOffset;

    public MatchingEngine(MatchingEngineSnapshot snapshot, LogWriter logWriter) {
        this.logWriter = logWriter;
        if (snapshot != null) {
            this.logSequence.set(snapshot.getLogSequence());
            this.commandOffset = snapshot.getCommandOffset();
            this.accountBook = new AccountBook(snapshot.getAccounts(), logWriter, logSequence);
            if (snapshot.getOrderBookSnapshots() != null) {
                snapshot.getOrderBookSnapshots().forEach(x -> {
                    OrderBook orderBook = new OrderBook( x, logWriter, accountBook, productBook, logSequence);
                    this.orderBooks.put(orderBook.getProductId(), orderBook);
                });
            }
        } else {
            this.accountBook = new AccountBook(null, logWriter, logSequence);
        }
    }

    public void executeCommand(DepositCommand command) {
        commandOffset = command.getOffset();
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId());
    }

    public void executeCommand(PlaceOrderCommand command) {
        commandOffset = command.getOffset();
        OrderBook orderBook = createOrderBook(command.getProductId());
        orderBook.placeOrder(new Order(command));
    }

    private OrderBook createOrderBook(String productId) {
        OrderBook orderBook = orderBooks.get(productId);
        if (orderBook == null) {
            orderBook = new OrderBook(productId, logWriter, accountBook, productBook, logSequence);
            orderBooks.put(productId, orderBook);
        }
        return orderBook;
    }

    public void executeCommand(CancelOrderCommand command) {
        commandOffset = command.getOffset();
        orderBooks.get(command.getProductId()).cancelOrder(command.getOrderId());
    }

    public MatchingEngineSnapshot takeSnapshot() {
        List<Product> products = this.productBook.getProducts().values().stream()
                .map(Product::copy)
                .collect(Collectors.toList());
        List<Account> accounts = this.accountBook.getAccounts().values().stream()
                .map(Account::copy)
                .collect(Collectors.toList());
        List<OrderBookSnapshot> orderBookSnapshots = this.orderBooks.values().stream()
                .map(OrderBook::takeSnapshot)
                .collect(Collectors.toList());

        MatchingEngineSnapshot snapshot = new MatchingEngineSnapshot();
        snapshot.setProducts(products);
        snapshot.setAccounts(accounts);
        snapshot.setOrderBookSnapshots(orderBookSnapshots);
        snapshot.setLogSequence(logSequence.get());
        snapshot.setCommandOffset(commandOffset);
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
