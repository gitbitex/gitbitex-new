package com.gitbitex.matchingengine;


import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.log.OrderRejectedLog.RejectReason;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BookPage implements Serializable {
    private final String productId;
    private final TreeMap<BigDecimal, PageLine> lineByPrice;
    private final LinkedHashMap<String, Order> orderById = new LinkedHashMap<>();
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final LogWriter logWriter;
    private final AccountBook accountBook;
    private final ProductBook productBook;

    public BookPage(String productId, Comparator<BigDecimal> priceComparator,
                    AtomicLong tradeId, AtomicLong sequence,
                    AccountBook accountBook, ProductBook productBook,
                    LogWriter logWriter,List<Order> orders
    ) {
        this.lineByPrice = new TreeMap<>(priceComparator);
        this.productId = productId;
        this.tradeId = tradeId;
        this.sequence = sequence;
        this.logWriter = logWriter;
        this.accountBook = accountBook;
        this.productBook = productBook;
        if (orders!=null){
            orders.forEach(this::addOrder);
        }
    }

    private boolean holdOrderFunds(Order takerOrder, Product product) {
        String takerUserId = takerOrder.getUserId();
        String baseCurrency = product.getBaseCurrency();
        String quoteCurrency = product.getQuoteCurrency();

        if (takerOrder.getSide() == OrderSide.BUY) {
            if (takerOrder.getFunds().compareTo(accountBook.getAvailable(takerUserId, quoteCurrency)) > 0) {
                return false;
            }
            accountBook.hold(takerUserId, quoteCurrency, takerOrder.getFunds());
        } else {
            if (takerOrder.getSize().compareTo(accountBook.getAvailable(takerUserId, baseCurrency)) > 0) {
                return false;
            }
            accountBook.hold(takerUserId, baseCurrency, takerOrder.getSize());
        }
        return true;
    }

    public void executeCommand(Order takerOrder, BookPage takerPage) {
        Product product = productBook.getProduct(productId);
        String takerUserId = takerOrder.getUserId();
        String baseCurrency = product.getBaseCurrency();
        String quoteCurrency = product.getQuoteCurrency();

        if (!holdOrderFunds(takerOrder, product)) {
            logWriter.add(orderRejectedLog(takerOrder, RejectReason.INSUFFICIENT_FUNDS));
            return;
        }

        logWriter.add(orderReceivedLog(takerOrder));

        MATCHING:
        for (PageLine line : lineByPrice.values()) {
            BigDecimal price = line.getPrice();

            // check whether there is price crossing between the taker and the maker
            if (!isPriceCrossed(takerOrder, price)) {
                break;
            }

            for (Order makerOrder : line.getOrders()) {
                // get taker size
                BigDecimal takerSize;
                if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getType() == OrderType.MARKET) {
                    // The market order does not specify a price, so the size of the maker order needs to be
                    // calculated by the price of the maker order
                    takerSize = takerOrder.getFunds().divide(price, 4, RoundingMode.DOWN);
                    if (takerSize.compareTo(BigDecimal.ZERO) == 0) {
                        break MATCHING;
                    }
                } else {
                    takerSize = takerOrder.getSize();
                }

                // take the minimum size of taker and maker as trade size
                BigDecimal tradeSize = takerSize.min(makerOrder.getSize());
                BigDecimal tradeFunds = tradeSize.multiply(price);

                // adjust the size/funds
                takerOrder.setSize(takerOrder.getSize().subtract(tradeSize));
                makerOrder.setSize(makerOrder.getSize().subtract(tradeSize));
                if (takerOrder.getSide() == OrderSide.BUY) {
                    takerOrder.setFunds(takerOrder.getFunds().subtract(tradeFunds));
                } else {
                    makerOrder.setFunds(makerOrder.getFunds().subtract(tradeFunds));
                }

                // create a new match log
                logWriter.add(orderMatchLog(takerOrder, makerOrder, tradeSize, tradeId.incrementAndGet()));
                accountBook.exchange(takerUserId, makerOrder.getUserId(), baseCurrency, quoteCurrency, takerOrder.getSide(), tradeSize, tradeFunds);

                // if the maker order is fully filled, remove it from the order book.
                if (makerOrder.getSize().compareTo(BigDecimal.ZERO) == 0) {
                    line.removeOrderById(makerOrder.getOrderId());
                    logWriter.add(orderDoneLog(makerOrder, OrderDoneLog.DoneReason.FILLED));
                    unholdOrderFunds(makerOrder, baseCurrency, quoteCurrency);
                }
            }

            // remove line with empty order list
            if (line.getOrders().isEmpty()) {
                lineByPrice.remove(line.getPrice());
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,
        // Note: The market order will never be added to the order book, and the market order without fully filled
        // will be cancelled
        if (takerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
            if (takerOrder.getType() == OrderType.LIMIT) {
                takerPage.addOrder(takerOrder);
                logWriter.add(orderOpenLog(takerOrder));
            } else if (takerOrder.getType() == OrderType.MARKET) {
                logWriter.add(orderDoneLog(takerOrder, OrderDoneLog.DoneReason.CANCELLED));
                unholdOrderFunds(takerOrder, baseCurrency, quoteCurrency);
            }
        } else {
            logWriter.add(orderDoneLog(takerOrder, OrderDoneLog.DoneReason.FILLED));
            unholdOrderFunds(takerOrder, baseCurrency, quoteCurrency);
        }
    }

    public void cancelOrder(String orderId) {
        Product product = productBook.getProduct(productId);
        Order order = orderById.get(orderId);
        if (order == null) {
            return;
        }

        removeOrderById(orderId);

        logWriter.add(orderDoneLog(order, OrderDoneLog.DoneReason.CANCELLED));
        unholdOrderFunds(order, product.getBaseCurrency(), product.getQuoteCurrency());
    }

    public PageLine addOrder(Order order) {
        PageLine line = lineByPrice.computeIfAbsent(order.getPrice(),
                k -> new PageLine(order.getPrice()));
        line.addOrder(order);
        orderById.put(order.getOrderId(), order);
        return line;
    }

    public PageLine removeOrderById(String orderId) {
        Order order = orderById.remove(orderId);
        if (order == null) {
            return null;
        }

        PageLine line = lineByPrice.get(order.getPrice());
        line.removeOrderById(order.getOrderId());
        if (line.getOrders().isEmpty()) {
            lineByPrice.remove(order.getPrice());
        }
        return line;
    }

    public Collection<PageLine> getLines() {
        return this.lineByPrice.values();
    }

    public Collection<Order> getOrders() {
        return this.orderById.values();
    }

    private boolean isPriceCrossed(Order takerOrder, BigDecimal makerOrderPrice) {
        if (takerOrder.getType() == OrderType.MARKET) {
            return true;
        }
        if (takerOrder.getSide() == OrderSide.BUY) {
            return takerOrder.getPrice().compareTo(makerOrderPrice) >= 0;
        } else {
            return takerOrder.getPrice().compareTo(makerOrderPrice) <= 0;
        }
    }

    private void unholdOrderFunds(Order makerOrder, String baseCurrency, String quoteCurrency) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), quoteCurrency, makerOrder.getFunds());
            }
        } else {
            if (makerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), baseCurrency, makerOrder.getSize());
            }
        }
    }

    private OrderRejectedLog orderRejectedLog(Order order, RejectReason rejectReason) {
        OrderRejectedLog log = new OrderRejectedLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setUserId(order.getUserId());
        log.setPrice(order.getPrice());
        log.setFunds(order.getFunds());
        log.setSide(order.getSide());
        log.setOrderId(order.getOrderId());
        log.setTime(new Date());
        log.setRejectReason(rejectReason);
        return log;
    }

    private OrderReceivedLog orderReceivedLog(Order order) {
        OrderReceivedLog log = new OrderReceivedLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setSequence(sequence.incrementAndGet());
        log.setUserId(order.getUserId());
        log.setPrice(order.getPrice());
        log.setFunds(order.getFunds());
        log.setSide(order.getSide());
        log.setOrderId(order.getOrderId());
        log.setTime(new Date());
        return log;
    }

    private OrderOpenLog orderOpenLog(Order order) {
        OrderOpenLog log = new OrderOpenLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setRemainingSize(order.getSize());
        log.setPrice(order.getPrice());
        log.setSide(order.getSide());
        log.setOrderId(order.getOrderId());
        log.setUserId(order.getUserId());
        log.setTime(new Date());
        return log;
    }

    private OrderMatchLog orderMatchLog(Order takerOrder, Order makerOrder, BigDecimal size, long tradeId) {
        OrderMatchLog log = new OrderMatchLog();
        log.setSequence(sequence.incrementAndGet());
        log.setTradeId(tradeId);
        log.setProductId(productId);
        log.setTakerOrderId(takerOrder.getOrderId());
        log.setMakerOrderId(makerOrder.getOrderId());
        log.setTakerUserId(takerOrder.getUserId());
        log.setMakerUserId(makerOrder.getUserId());
        log.setPrice(makerOrder.getPrice());
        log.setSize(size);
        log.setFunds(makerOrder.getPrice().multiply(size));
        log.setSide(makerOrder.getSide());
        log.setTime(takerOrder.getTime());
        return log;
    }

    private OrderDoneLog orderDoneLog(Order order, OrderDoneLog.DoneReason doneReason) {
        OrderDoneLog log = new OrderDoneLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        if (order.getType() != OrderType.MARKET) {
            log.setRemainingSize(order.getSize());
            log.setPrice(order.getPrice());
        }
        log.setRemainingFunds(order.getFunds());
        log.setRemainingSize(order.getSize());
        log.setSide(order.getSide());
        log.setOrderId(order.getOrderId());
        log.setUserId(order.getUserId());
        log.setDoneReason(doneReason);
        log.setOrderType(order.getType());
        log.setTime(new Date());
        return log;
    }
}
