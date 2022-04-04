package com.gitbitex.matchingengine;

import com.gitbitex.entity.Order;
import com.gitbitex.entity.Order.OrderSide;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.marketmessage.Level3OrderBookSnapshot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OrderBook {
    private final String productId;
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final BookPage asks;
    private final BookPage bids;
    private long matchingCommandOffset;
    private long matchingLogOffset;

    public OrderBook(String productId) {
        this(productId, new AtomicLong(), new AtomicLong(), 0, 0, new ArrayList<>(), new ArrayList<>());
    }

    public OrderBook(String productId, Level3OrderBookSnapshot snapshot) {
        this(productId, new AtomicLong(snapshot.getLastTradeId()), new AtomicLong(snapshot.getSequence()),
                snapshot.getOrderBookCommandOffset(),
                snapshot.getOrderBookLogOffset(),
                snapshot.getAsks().stream()
                        .map(Level3OrderBookSnapshot.Level3SnapshotLine::getOrder)
                        .peek(x -> x.setSide(OrderSide.SELL))
                        .collect(Collectors.toList()),
                snapshot.getBids().stream()
                        .map(Level3OrderBookSnapshot.Level3SnapshotLine::getOrder)
                        .peek(x -> x.setSide(OrderSide.BUY))
                        .collect(Collectors.toList()));
    }

    public OrderBook(String productId, AtomicLong tradeId, AtomicLong sequence, long matchingCommandOffset,
                     long matchingLogOffset, List<BookOrder> askOrders, List<BookOrder> bidOrders) {
        this.productId = productId;
        this.tradeId = tradeId;
        this.sequence = sequence;
        this.matchingCommandOffset = matchingCommandOffset;
        this.matchingLogOffset = matchingLogOffset;
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, sequence, askOrders);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, sequence, bidOrders);
    }

    public List<OrderBookLog> executeCommand(NewOrderCommand command) {
        Order order = command.getOrder();
        if (order.getSide() == OrderSide.BUY) {
            return (asks.executeCommand(command, bids));
        } else {
            return (bids.executeCommand(command, asks));
        }
    }

    public OrderBookLog executeCommand(CancelOrderCommand message) {
        String orderId = message.getOrderId();
        BookOrder order = asks.getOrderById(orderId);
        if (order == null) {
            order = bids.getOrderById(orderId);
        }
        if (order == null) {
            return null;
        }

        return (order.getSide() == OrderSide.BUY ? bids : asks).executeCommand(message);
    }

    public PageLine restoreLog(OrderReceivedLog message) {
        this.sequence.set(message.getSequence());
        this.matchingLogOffset = message.getOffset();
        this.matchingCommandOffset = message.getCommandOffset();
        return null;
    }

    public PageLine restoreLog(OrderOpenLog message) {
        this.sequence.set(message.getSequence());
        this.matchingLogOffset = message.getOffset();
        this.matchingCommandOffset = message.getCommandOffset();
        BookOrder order = new BookOrder();
        order.setOrderId(message.getOrderId());
        order.setPrice(message.getPrice());
        order.setSize(message.getRemainingSize());
        order.setSide(message.getSide());
        order.setUserId(message.getUserId());
        return this.addOrder(order);
    }

    public PageLine restoreLog(OrderMatchLog message) {
        this.sequence.set(message.getSequence());
        this.matchingLogOffset = message.getOffset();
        this.matchingCommandOffset = message.getCommandOffset();
        this.tradeId.set(message.getTradeId());
        return this.decreaseOrderSize(message.getMakerOrderId(), message.getSide(), message.getSize());
    }

    public PageLine restoreLog(OrderDoneLog message) {
        this.sequence.set(message.getSequence());
        this.matchingLogOffset = message.getOffset();
        this.matchingCommandOffset = message.getCommandOffset();
        if (message.getPrice() != null) {
            return this.removeOrderById(message.getOrderId(), message.getSide());
        }
        return null;
    }

    private PageLine addOrder(BookOrder order) {
        return (order.getSide() == OrderSide.BUY ? bids : asks).addOrder(order);
    }

    private PageLine decreaseOrderSize(String orderId, OrderSide side, BigDecimal size) {
        return (side == OrderSide.BUY ? bids : asks).decreaseOrderSize(orderId, size);
    }

    private PageLine removeOrderById(String orderId, OrderSide side) {
        return (side == OrderSide.BUY ? bids : asks).removeOrderById(orderId);
    }

    public BookOrder getOrderById(String orderId) {
        BookOrder order = this.asks.getOrderById(orderId);
        if (order != null) {
            return order;
        }
        return this.bids.getOrderById(orderId);
    }

    public OrderBook copy() {
        return new OrderBook(this.productId, new AtomicLong(this.tradeId.get()), new AtomicLong(this.sequence.get()),
                this.matchingCommandOffset, this.matchingLogOffset,
                this.asks.getOrders().stream().map(BookOrder::copy).collect(Collectors.toList()),
                this.bids.getOrders().stream().map(BookOrder::copy).collect(Collectors.toList()));
    }
}
