package com.gitbitex.matchingengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
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

    public void restoreLog(AccountChangeMessage log) {
        accountBook.restoreLog(log);
    }

    public void restore(OrderOpenMessage log) {
        orderBooks.get(log.getProductId()).restoreLog(log);
    }

    public void restore(OrderMatchLog log) {
        orderBooks.get(log.getProductId()).restoreLog(log);
    }

    public void restore(OrderDoneMessage log) {
        orderBooks.get(log.getProductId()).restoreLog(log);
    }

    public void restore(OrderReceivedMessage log) {
        orderBooks.get(log.getProductId()).restoreLog(log);
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

}
