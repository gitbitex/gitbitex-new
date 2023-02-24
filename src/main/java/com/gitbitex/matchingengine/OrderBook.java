package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class OrderBook {
    private final String productId;
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final BookPage asks;
    private final BookPage bids;
    //private final LinkedHashSet<String> orderIds = new LinkedHashSet<>();
    //private final EngineLogger engineLogger;
    //private final AccountBook accountBook;
    private long commandOffset;
    private long logOffset;
    private boolean stable;

    public OrderBook(String productId, LogWriter logWriter, AccountBook accountBook, AtomicLong sequence) {
        this.productId = productId;
        this.tradeId = new AtomicLong();
        this.sequence = sequence;
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, sequence, accountBook, logWriter);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, sequence, accountBook, logWriter);
    }

    public OrderBook(String productId, OrderBookSnapshot snapshot, LogWriter logWriter, AccountBook accountBook,
        AtomicLong logSequence) {
        this.sequence = logSequence;
        this.productId = productId;
        if (snapshot != null) {
            this.tradeId = new AtomicLong(snapshot.getTradeId());
        } else {
            this.tradeId = new AtomicLong();
        }
        this.asks = new BookPage(productId, Comparator.naturalOrder(), tradeId, sequence, accountBook, logWriter);
        this.bids = new BookPage(productId, Comparator.reverseOrder(), tradeId, sequence, accountBook, logWriter);
        if (snapshot != null) {
            if (snapshot.getAsks() != null) {
                snapshot.getAsks().forEach(this.asks::addOrder);
            }
            if (snapshot.getBids() != null) {
                snapshot.getBids().forEach(this.bids::addOrder);
            }
        }
    }

    public static void main(String[] a) {
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000,
            0.01);
        bloomFilter.put("1");
        System.out.println(bloomFilter.mightContain("1"));
    }

    public OrderBookSnapshot takeSnapshot() {
        List<BookOrder> askOrders = this.asks.getOrders().stream()
            .map(BookOrder::copy)
            .collect(Collectors.toList());
        List<BookOrder> bidOrders = this.bids.getOrders().stream()
            .map(BookOrder::copy)
            .collect(Collectors.toList());

        OrderBookSnapshot orderBookSnapshot = new OrderBookSnapshot();
        orderBookSnapshot.setProductId(productId);
        orderBookSnapshot.setTradeId(tradeId.get());
        orderBookSnapshot.setAsks(askOrders);
        orderBookSnapshot.setBids(bidOrders);
        return orderBookSnapshot;
    }

    public void executeCommand(PlaceOrderCommand command) {
        Order order = command.getOrder();

        // ensure that the order will not be processed repeatedly
        /*if (!putOrderIdIfAbsent(order.getOrderId())) {
            logger.warn("received repeated order: {}", JSON.toJSONString(order));
            return;
        }*/

        if (order.getSide() == OrderSide.BUY) {
            asks.executeCommand(command, bids);
        } else {
            bids.executeCommand(command, asks);
        }
    }

    public void executeCommand(CancelOrderCommand message) {
        String orderId = message.getOrderId();
        BookOrder order = asks.getOrderById(orderId);
        if (order == null) {
            order = bids.getOrderById(orderId);
        }
        if (order == null) {
            return;
        }

        (order.getSide() == OrderSide.BUY ? bids : asks).executeCommand(message);
    }

    public PageLine restoreLog(OrderReceivedMessage log) {
        this.sequence.set(log.getSequence());
        this.logOffset = log.getOffset();
        this.commandOffset = log.getCommandOffset();
        this.stable = log.isCommandFinished();
        //putOrderIdIfAbsent(log.getOrder().getOrderId());
        return null;
    }

    public PageLine restoreLog(OrderOpenMessage log) {
        this.sequence.set(log.getSequence());
        this.logOffset = log.getOffset();
        this.commandOffset = log.getCommandOffset();
        this.stable = log.isCommandFinished();
        BookOrder order = new BookOrder();
        order.setOrderId(log.getOrderId());
        order.setPrice(log.getPrice());
        order.setSize(log.getRemainingSize());
        order.setSide(log.getSide());
        order.setUserId(log.getUserId());
        return this.addOrder(order);
    }

    public PageLine restoreLog(OrderMatchLog log) {
        this.sequence.set(log.getSequence());
        this.logOffset = log.getOffset();
        this.commandOffset = log.getCommandOffset();
        this.tradeId.set(log.getTradeId());
        this.stable = log.isCommandFinished();
        return this.decreaseOrderSize(log.getMakerOrderId(), log.getSide(), log.getSize());
    }

    public PageLine restoreLog(OrderDoneMessage log) {
        this.sequence.set(log.getSequence());
        this.logOffset = log.getOffset();
        this.commandOffset = log.getCommandOffset();
        this.stable = log.isCommandFinished();
        if (log.getPrice() != null) {
            return this.removeOrderById(log.getOrderId(), log.getSide());
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

    /*private boolean putOrderIdIfAbsent(String orderId) {
        if (orderIds.size() == 100000) {
            for (int i = 0; i < 100; i++) {
                //orderIds.remove(orderIds.de)
            }
        }
        return (orderIds.add(orderId));
    }*/
}
