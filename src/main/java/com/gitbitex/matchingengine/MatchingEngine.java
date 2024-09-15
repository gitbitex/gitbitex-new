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

        restoreSnapshot(stateStore, messageSender);
    }

    public void executeCommand(Command command, long offset) {
        commandProcessedCounter.increment();

        sendCommandStartMessage(command, offset);
        if (command instanceof PlaceOrderCommand placeOrderCommand) {
            executeCommand(placeOrderCommand);
        } else if (command instanceof CancelOrderCommand cancelOrderCommand) {
            executeCommand(cancelOrderCommand);
        } else if (command instanceof DepositCommand depositCommand) {
            executeCommand(depositCommand);
        } else if (command instanceof PutProductCommand putProductCommand) {
            executeCommand(putProductCommand);
        } else {
            logger.warn("Unhandled command: {} {}", command.getClass().getName(), JSON.toJSONString(command));
        }
        sendCommandEndMessage(command, offset);
    }

    private void executeCommand(DepositCommand command) {
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId());
    }

    private void executeCommand(PutProductCommand command) {
        productBook.putProduct(new Product(command));
        createOrderBook(command.getProductId());
    }

    private void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }
        orderBook.placeOrder(new Order(command));
    }

    private void executeCommand(CancelOrderCommand command) {
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            logger.warn("no such order book: {}", command.getProductId());
            return;
        }
        orderBook.cancelOrder(command.getOrderId());
    }

    private void sendCommandStartMessage(Command command, long offset) {
        CommandStartMessage message = new CommandStartMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setCommandOffset(offset);
        messageSender.send(message);
    }

    private void sendCommandEndMessage(Command command, long offset) {
        CommandEndMessage message = new CommandEndMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setCommandOffset(offset);
        messageSender.send(message);
    }

    private void restoreSnapshot(EngineSnapshotManager stateStore, MessageSender messageSender) {
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

    private void createOrderBook(String productId) {
        if (orderBooks.containsKey(productId)) {
            return;
        }
        OrderBook orderBook = new OrderBook(productId, 0, 0, 0, accountBook, productBook, messageSender, messageSequence);
        orderBooks.put(productId, orderBook);
    }

}
