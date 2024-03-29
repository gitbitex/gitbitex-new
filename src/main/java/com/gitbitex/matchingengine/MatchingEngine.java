package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.matchingengine.command.*;
import com.gitbitex.matchingengine.message.CommandEndMessage;
import com.gitbitex.matchingengine.message.CommandStartMessage;
import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import com.gitbitex.matchingengine.snapshot.EngineState;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks = new HashMap<>();
    private final EngineSnapshotManager stateStore;
    private final Counter commandProcessedCounter;
    private final AtomicLong messageSequence = new AtomicLong();
    private final MessageSender messageSender;
    private final ProductBook productBook;
    private final AccountBook accountBook;
    @Getter
    private Long startupCommandOffset;

    public MatchingEngine(EngineSnapshotManager stateStore, MessageSender messageSender) {
        this.stateStore = stateStore;
        this.messageSender = messageSender;
        this.commandProcessedCounter = Counter.builder("gbe.matching-engine.command.processed")
                .register(Metrics.globalRegistry);
        this.productBook = new ProductBook(messageSender, this.messageSequence);
        this.accountBook = new AccountBook(messageSender, this.messageSequence);


        logger.info("restoring snapshot");
        stateStore.runInSession(session -> {
            // restore engine states
            EngineState engineState = stateStore.getEngineState(session);
            if (engineState == null) {
                logger.info("no snapshot found");
                return;
            }

            logger.info("snapshot found, state: {}", JSON.toJSONString(engineState));


            if (engineState.getCommandOffset() != null) {
                this.startupCommandOffset = engineState.getCommandOffset();
            }
            if (engineState.getMessageSequence() != null) {
                this.messageSequence.set(engineState.getMessageSequence());
            }

            // restore product book
            stateStore.getProducts(session).forEach(productBook::addProduct);

            // restore account book
            stateStore.getAccounts(session).forEach(accountBook::add);

            // restore order books
            for (Product product : this.productBook.getAllProducts()) {
                OrderBook orderBook = new OrderBook(product.getId(),
                        engineState.getOrderSequences().getOrDefault(product.getId(), 0L),
                        engineState.getTradeSequences().getOrDefault(product.getId(), 0L),
                        engineState.getOrderBookSequences().getOrDefault(product.getId(), 0L),
                        accountBook, productBook, messageSender, this.messageSequence);
                orderBooks.put(orderBook.getProductId(), orderBook);


                for (Order order : stateStore.getOrders(session, product.getId())) {
                    orderBook.addOrder(order);
                }
            }
        });
        logger.info("snapshot restored");
    }

    private CommandStartMessage commandStartMessage(Command command) {
        CommandStartMessage message = new CommandStartMessage();
        message.setSequence(messageSequence.incrementAndGet());
        return message;
    }

    private CommandEndMessage commandEndMessage(Command command) {
        CommandEndMessage message = new CommandEndMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setCommandOffset(command.getOffset());
        return message;
    }

    public void executeCommand(DepositCommand command) {
        commandProcessedCounter.increment();

        messageSender.send(commandStartMessage(command));
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId());
        messageSender.send(commandEndMessage(command));
    }

    public void executeCommand(PutProductCommand command) {
        commandProcessedCounter.increment();

        messageSender.send(commandStartMessage(command));
        productBook.putProduct(new Product(command));
        createOrderBook(command.getProductId());
        messageSender.send(commandEndMessage(command));
    }

    public void executeCommand(PlaceOrderCommand command) {
        commandProcessedCounter.increment();
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }

        messageSender.send(commandStartMessage(command));
        orderBook.placeOrder(new Order(command));
        messageSender.send(commandEndMessage(command));
    }

    public void executeCommand(CancelOrderCommand command) {
        commandProcessedCounter.increment();
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }

        messageSender.send(commandStartMessage(command));
        orderBook.cancelOrder(command.getOrderId());
        messageSender.send(commandEndMessage(command));
    }

    private void createOrderBook(String productId) {
        if (orderBooks.containsKey(productId)) {
            return;
        }
        OrderBook orderBook = new OrderBook(productId, 0, 0, 0, accountBook, productBook, messageSender, messageSequence);
        orderBooks.put(productId, orderBook);
    }

}
