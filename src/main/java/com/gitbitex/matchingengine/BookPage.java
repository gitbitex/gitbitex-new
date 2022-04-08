package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;

public class BookPage {
    private final String productId;
    private final TreeMap<BigDecimal, PageLine> lines;
    private final LinkedHashMap<String, BookOrder> orders = new LinkedHashMap<>();
    private final AtomicLong tradeId;
    private final AtomicLong sequence;

    public BookPage(String productId, Comparator<BigDecimal> priceComparator, AtomicLong tradeId, AtomicLong sequence,
        List<BookOrder> orders) {
        this.lines = new TreeMap<>(priceComparator);
        this.productId = productId;
        this.tradeId = tradeId;
        this.sequence = sequence;
        orders.forEach(this::addOrder);
    }

    public List<OrderBookLog> executeCommand(NewOrderCommand command, BookPage oppositePage) {
        List<OrderBookLog> logs = new ArrayList<>();

        Order order = command.getOrder();
        logs.add(orderReceivedLog(command.getOffset(), order));
        if (order.getStatus() != Order.OrderStatus.NEW) {
            logs.get(0).setCommandFinished(true);
            return logs;
        }

        BookOrder takerOrder = new BookOrder(order);

        // If it's a Market-Buy order, set price to infinite high, and if it's market-sell,
        // set price to zero, which ensures that prices will cross.
        if (takerOrder.getType() == Order.OrderType.MARKET) {
            if (takerOrder.getSide() == Order.OrderSide.BUY) {
                takerOrder.setPrice(BigDecimal.valueOf(Long.MAX_VALUE));
            } else {
                takerOrder.setPrice(BigDecimal.ZERO);
            }
        }

        List<BookOrder> matchedOrders = new ArrayList<>();

        MATCHING:
        for (PageLine line : lines.values()) {
            BigDecimal price = line.getPrice();

            // check whether there is price crossing between the taker and the maker
            if (!isPriceCrossed(takerOrder, price)) {
                break;
            }

            // The post-only flag indicates that the order should only make liquidity. If any part of the order
            // results in taking liquidity, the order will be rejected and no part of it will execute.
            if (takerOrder.isPostOnly()) {
                logs.add(orderDoneLog(command.getOffset(), takerOrder));
                logs.get(logs.size() - 1).setCommandFinished(true);
                return logs;
            }

            for (BookOrder makerOrder : line.getOrders()) {
                // calculate the size of taker at current price
                BigDecimal takerSize;
                if (takerOrder.getSide() == Order.OrderSide.BUY && takerOrder.getType() == Order.OrderType.MARKET) {
                    takerSize = takerOrder.getFunds().divide(price, 4, RoundingMode.DOWN);
                } else {
                    takerSize = takerOrder.getSize();
                }

                if (takerSize.compareTo(BigDecimal.ZERO) == 0) {
                    break MATCHING;
                }

                // Take the minimum size of taker and maker as trade size
                BigDecimal dealSize = takerSize.min(makerOrder.getSize());

                // adjust the size of taker order
                takerOrder.setSize(takerOrder.getSize().subtract(dealSize));

                // only market-buy order use funds
                if (takerOrder.getSide() == Order.OrderSide.BUY && takerOrder.getType() == Order.OrderType.MARKET) {
                    takerOrder.setFunds(takerOrder.getFunds().subtract(dealSize.multiply(price)));
                }

                // adjust the size of maker order
                makerOrder.setSize(makerOrder.getSize().subtract(dealSize));

                // create a new match log
                logs.add(orderMatchLog(command.getOffset(), takerOrder, makerOrder, dealSize));

                matchedOrders.add(makerOrder);
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,
        // Note: The market order will never be added to the order book, and the market order without fully filled
        // will be marked as cancelled
        if (takerOrder.getType() == Order.OrderType.LIMIT && takerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
            oppositePage.addOrder(takerOrder);
            logs.add(orderOpenLog(command.getOffset(), takerOrder));
        } else {
            logs.add(orderDoneLog(command.getOffset(), takerOrder));
        }

        // Check all maker orders that have traded, if the marker order is fully filled, remove it from the order book
        for (BookOrder makerOrder : matchedOrders) {
            if (makerOrder.getSize().compareTo(BigDecimal.ZERO) == 0) {
                removeOrderById(makerOrder.getOrderId());
                logs.add(orderDoneLog(command.getOffset(), makerOrder));
            }
        }

        logs.get(logs.size() - 1).setCommandFinished(true);
        return logs;
    }

    public OrderDoneLog executeCommand(CancelOrderCommand command) {
        String orderId = command.getOrderId();
        BookOrder order = orders.get(orderId);
        if (order == null) {
            return null;
        }

        removeOrderById(orderId);

        OrderDoneLog message = orderDoneLog(command.getOffset(), order);
        message.setCommandFinished(true);
        return message;
    }

    public PageLine addOrder(BookOrder order) {
        PageLine line = lines.computeIfAbsent(order.getPrice(), k -> new PageLine(order.getPrice(), order.getSide()));
        line.addOrder(order);
        orders.put(order.getOrderId(), order);
        return line;
    }

    public PageLine decreaseOrderSize(String orderId, BigDecimal size) {
        BookOrder order = orders.get(orderId);
        PageLine line = lines.get(order.getPrice());
        line.decreaseOrderSize(orderId, size);
        return line;
    }

    public PageLine removeOrderById(String orderId) {
        BookOrder order = orders.remove(orderId);
        if (order == null) {
            return null;
        }

        PageLine line = lines.get(order.getPrice());
        line.removeOrderById(order.getOrderId());
        if (line.getOrders().isEmpty()) {
            lines.remove(order.getPrice());
        }
        return line;
    }

    public Collection<PageLine> getLines() {
        return this.lines.values();
    }

    public Collection<BookOrder> getOrders() {
        return this.orders.values();
    }

    public BookOrder getOrderById(String orderId) {
        return this.orders.get(orderId);
    }

    private boolean isPriceCrossed(BookOrder takerOrder, BigDecimal makerOrderPrice) {
        return ((takerOrder.getSide() == Order.OrderSide.BUY && takerOrder.getPrice().compareTo(makerOrderPrice) >= 0)
            ||
            (takerOrder.getSide() == Order.OrderSide.SELL && takerOrder.getPrice().compareTo(makerOrderPrice) <= 0));
    }

    private OrderDoneLog.DoneReason determineDoneReason(BookOrder order) {
        if (order.getType() == OrderType.MARKET && order.getSide() == OrderSide.BUY) {
            if (order.getFunds().compareTo(BigDecimal.ZERO) > 0) {
                return OrderDoneLog.DoneReason.CANCELLED;
            }
        }

        if (order.getSize().compareTo(BigDecimal.ZERO) > 0) {
            return OrderDoneLog.DoneReason.CANCELLED;
        }
        return OrderDoneLog.DoneReason.FILLED;
    }

    private OrderReceivedLog orderReceivedLog(long commandOffset, Order order) {
        OrderReceivedLog orderReceivedLog = new OrderReceivedLog();
        orderReceivedLog.setSequence(sequence.incrementAndGet());
        orderReceivedLog.setCommandOffset(commandOffset);
        orderReceivedLog.setProductId(productId);
        orderReceivedLog.setOrder(order);
        return orderReceivedLog;
    }

    private OrderOpenLog orderOpenLog(long commandOffset, BookOrder takerOrder) {
        OrderOpenLog message = new OrderOpenLog();
        message.setCommandOffset(commandOffset);
        message.setSequence(sequence.incrementAndGet());
        message.setProductId(productId);
        message.setRemainingSize(takerOrder.getSize());
        message.setPrice(takerOrder.getPrice());
        message.setSide(takerOrder.getSide());
        message.setOrderId(takerOrder.getOrderId());
        message.setUserId(takerOrder.getUserId());
        return message;
    }

    private OrderMatchLog orderMatchLog(long commandOffset, BookOrder takerOrder, BookOrder makerOrder,
        BigDecimal size) {
        OrderMatchLog message = new OrderMatchLog();
        message.setCommandOffset(commandOffset);
        message.setSequence(sequence.incrementAndGet());
        message.setTradeId(tradeId.incrementAndGet());
        message.setProductId(productId);
        message.setTakerOrderId(takerOrder.getOrderId());
        message.setMakerOrderId(makerOrder.getOrderId());
        message.setPrice(makerOrder.getPrice());
        message.setSize(size);
        message.setFunds(makerOrder.getPrice().multiply(size));
        message.setSide(makerOrder.getSide());
        message.setTime(new Date());
        return message;
    }

    private OrderDoneLog orderDoneLog(long commandOffset, BookOrder order) {
        OrderDoneLog message = new OrderDoneLog();
        message.setCommandOffset(commandOffset);
        message.setSequence(sequence.incrementAndGet());
        message.setProductId(productId);
        if (order.getType() != OrderType.MARKET) {
            message.setRemainingSize(order.getSize());
            message.setPrice(order.getPrice());
        }
        message.setSide(order.getSide());
        message.setOrderId(order.getOrderId());
        message.setUserId(order.getUserId());
        message.setDoneReason(determineDoneReason(order));
        message.setOrderType(order.getType());
        return message;
    }
}
