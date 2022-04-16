package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BookPage implements Serializable {
    private final String productId;
    private final TreeMap<BigDecimal, PageLine> lineByPrice;
    private final LinkedHashMap<String, BookOrder> orderById = new LinkedHashMap<>();
    private final AtomicLong tradeId;
    private final AtomicLong sequence;

    public BookPage(String productId, Comparator<BigDecimal> priceComparator, AtomicLong tradeId, AtomicLong sequence,
                    List<BookOrder> orders) {
        this.lineByPrice = new TreeMap<>(priceComparator);
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

        BookOrder takerOrder = new BookOrder();
        BeanUtils.copyProperties(order, takerOrder);

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
        for (PageLine line : lineByPrice.values()) {
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

            for (BookOrder makerOrder : line.getOrderById()) {
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
                BigDecimal tradeSize = takerSize.min(makerOrder.getSize());

                // adjust the size of taker order
                takerOrder.setSize(takerOrder.getSize().subtract(tradeSize));

                // only market-buy order use funds
                if (takerOrder.getSide() == Order.OrderSide.BUY && takerOrder.getType() == Order.OrderType.MARKET) {
                    takerOrder.setFunds(takerOrder.getFunds().subtract(tradeSize.multiply(price)));
                }

                // adjust the size of maker order
                makerOrder.setSize(makerOrder.getSize().subtract(tradeSize));

                // create a new match log
                logs.add(orderMatchLog(command.getOffset(), takerOrder, makerOrder, tradeSize));

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
        BookOrder order = orderById.get(orderId);
        if (order == null) {
            return null;
        }

        removeOrderById(orderId);

        OrderDoneLog message = orderDoneLog(command.getOffset(), order);
        message.setCommandFinished(true);
        return message;
    }

    public PageLine addOrder(BookOrder order) {
        PageLine line = lineByPrice.computeIfAbsent(order.getPrice(), k -> new PageLine(order.getPrice(), order.getSide()));
        line.addOrder(order);
        orderById.put(order.getOrderId(), order);
        return line;
    }

    public PageLine decreaseOrderSize(String orderId, BigDecimal size) {
        BookOrder order = orderById.get(orderId);
        PageLine line = lineByPrice.get(order.getPrice());
        line.decreaseOrderSize(orderId, size);
        return line;
    }

    public PageLine removeOrderById(String orderId) {
        BookOrder order = orderById.remove(orderId);
        if (order == null) {
            return null;
        }

        PageLine line = lineByPrice.get(order.getPrice());
        line.removeOrderById(order.getOrderId());
        if (line.getOrderById().isEmpty()) {
            lineByPrice.remove(order.getPrice());
        }
        return line;
    }

    public Collection<PageLine> getLineByPrice() {
        return this.lineByPrice.values();
    }

    public Collection<BookOrder> getOrderById() {
        return this.orderById.values();
    }

    public BookOrder getOrderById(String orderId) {
        return this.orderById.get(orderId);
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
        OrderReceivedLog log = new OrderReceivedLog();
        log.setSequence(sequence.incrementAndGet());
        log.setCommandOffset(commandOffset);
        log.setProductId(productId);
        log.setOrder(order);
        log.setTime(new Date());
        return log;
    }

    private OrderOpenLog orderOpenLog(long commandOffset, BookOrder takerOrder) {
        OrderOpenLog log = new OrderOpenLog();
        log.setCommandOffset(commandOffset);
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setRemainingSize(takerOrder.getSize());
        log.setPrice(takerOrder.getPrice());
        log.setSide(takerOrder.getSide());
        log.setOrderId(takerOrder.getOrderId());
        log.setUserId(takerOrder.getUserId());
        log.setTime(new Date());
        return log;
    }

    private OrderMatchLog orderMatchLog(long commandOffset, BookOrder takerOrder, BookOrder makerOrder,
                                        BigDecimal size) {
        OrderMatchLog log = new OrderMatchLog();
        log.setCommandOffset(commandOffset);
        log.setSequence(sequence.incrementAndGet());
        log.setTradeId(tradeId.incrementAndGet());
        log.setProductId(productId);
        log.setTakerOrderId(takerOrder.getOrderId());
        log.setMakerOrderId(makerOrder.getOrderId());
        log.setPrice(makerOrder.getPrice());
        log.setSize(size);
        log.setFunds(makerOrder.getPrice().multiply(size));
        log.setSide(makerOrder.getSide());
        log.setTime(takerOrder.getTime());
        return log;
    }

    private OrderDoneLog orderDoneLog(long commandOffset, BookOrder order) {
        OrderDoneLog log = new OrderDoneLog();
        log.setCommandOffset(commandOffset);
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        if (order.getType() != OrderType.MARKET) {
            log.setRemainingSize(order.getSize());
            log.setPrice(order.getPrice());
        }
        log.setSide(order.getSide());
        log.setOrderId(order.getOrderId());
        log.setUserId(order.getUserId());
        log.setDoneReason(determineDoneReason(order));
        log.setOrderType(order.getType());
        log.setTime(new Date());
        return log;
    }
}
