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

    public MatchingEngine(MatchingEngineStateStore matchingEngineStateStore, DirtyObjectHandler dirtyObjectHandler) {
        this.dirtyObjectHandler = dirtyObjectHandler;
        this.accountBook=new AccountBook(dirtyObjectHandler);

        Long commandOffset = matchingEngineStateStore.getCommandOffset();
        if ( commandOffset == null) {
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
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
            command.getTransactionId(), command.getOffset());
    }

    public void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = createOrderBook(command.getProductId());
        DirtyObjectList<Object> dirtyObjects= orderBook.placeOrder(new Order(command));
        flush(command.getOffset(),dirtyObjects);
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
        orderBooks.get(command.getProductId()).cancelOrder(command.getOrderId(), command.getOffset());
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
