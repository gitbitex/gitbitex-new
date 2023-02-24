package com.gitbitex.matchingengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.L3OrderBook;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchingEngine {
    private final AccountBook accountBook;
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final LogWriter logWriter;
    private final AtomicLong logSequence = new AtomicLong();
    private long commandOffset;

    public MatchingEngine(EngineSnapshot snapshot, LogWriter logWriter) {
        this.logWriter = logWriter;
        if (snapshot != null) {
            this.logSequence.set(snapshot.getLogSequence());
            this.commandOffset = snapshot.getCommandOffset();
            this.accountBook = new AccountBook(snapshot.getAccountBookSnapshot(), logWriter, logSequence);
            if (snapshot.getOrderBookSnapshots() != null) {
                snapshot.getOrderBookSnapshots().forEach(x -> {
                    OrderBook orderBook = new OrderBook(x.getProductId(), x, logWriter, accountBook, logSequence);
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
        String productId = command.getOrder().getProductId();
        OrderBook orderBook = orderBooks.get(command.getOrder().getProductId());
        if (orderBook == null) {
            orderBook = new OrderBook(command.getOrder().getProductId(), logWriter, accountBook, logSequence);
            orderBooks.put(productId, orderBook);
        }
        orderBook.executeCommand(command);
    }

    public void executeCommand(CancelOrderCommand command) {
        commandOffset = command.getOffset();
        orderBooks.get(command.getProductId()).executeCommand(command);
    }

    public EngineSnapshot takeSnapshot() {
        AccountBookSnapshot accountBookSnapshot = accountBook.takeSnapshot();
        List<OrderBookSnapshot> orderBookSnapshots = this.orderBooks.values().stream().map(OrderBook::takeSnapshot)
            .collect(Collectors.toList());

        EngineSnapshot snapshot = new EngineSnapshot();
        snapshot.setAccountBookSnapshot(accountBookSnapshot);
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
