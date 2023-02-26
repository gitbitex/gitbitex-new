package com.gitbitex.matchingengine;


import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.log.*;
import com.gitbitex.matchingengine.log.OrderRejectedLog.RejectReason;
import lombok.Getter;
import org.aspectj.weaver.ast.Or;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BookPage implements Serializable {
    private final String productId;
    @Getter
    private final TreeMap<BigDecimal, LinkedHashMap<String, Order>> lineByPrice;
    private final LinkedHashMap<String, Order> orderById = new LinkedHashMap<>();
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final LogWriter logWriter;
    private final AccountBook accountBook;
    private final ProductBook productBook;

    public BookPage(String productId, Comparator<BigDecimal> priceComparator,
                    AtomicLong tradeId, AtomicLong sequence,
                    AccountBook accountBook, ProductBook productBook,
                    LogWriter logWriter, List<Order> orders
    ) {
        this.lineByPrice = new TreeMap<>(priceComparator);
        this.productId = productId;
        this.tradeId = tradeId;
        this.sequence = sequence;
        this.logWriter = logWriter;
        this.accountBook = accountBook;
        this.productBook = productBook;
        if (orders != null) {
            orders.forEach(this::addOrder);
        }
    }

    private boolean holdOrderFunds(Order takerOrder, Account takerBaseAccount, Account takerQuoteAccount) {
        if (takerOrder.getSide() == OrderSide.BUY) {
            if (takerQuoteAccount == null || takerQuoteAccount.getAvailable().compareTo(takerOrder.getFunds()) < 0) {
                return false;
            }
            accountBook.hold(takerQuoteAccount, takerOrder.getFunds());
        } else {
            if (takerBaseAccount == null || takerBaseAccount.getAvailable().compareTo(takerOrder.getSize()) < 0) {
                return false;
            }
            accountBook.hold(takerBaseAccount, takerOrder.getSize());
        }
        return true;
    }

    private void unholdOrderFunds(Order makerOrder, Account baseAccount, Account quoteAccount) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(quoteAccount, makerOrder.getFunds());
            }
        } else {
            if (makerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(baseAccount, makerOrder.getSize());
            }
        }
    }

    public void placeOrder(Order takerOrder, BookPage takerPage) {
        Product product = productBook.getProduct(productId);
        String takerUserId = takerOrder.getUserId();
        String baseCurrency = product.getBaseCurrency();
        String quoteCurrency = product.getQuoteCurrency();
        Map<String, Account> takerAccounts = accountBook.getAccountsByUserId(takerUserId);
        Account takerBaseAccount = takerAccounts.get(quoteCurrency);
        Account takerQuoteAccount = takerAccounts.get(quoteCurrency);

        if (!holdOrderFunds(takerOrder, takerBaseAccount, takerQuoteAccount)) {
            logWriter.add(orderRejectedLog(takerOrder, RejectReason.INSUFFICIENT_FUNDS));
            return;
        }

        // order received
        logWriter.add(orderReceivedLog(takerOrder));

        // let's start matching
        Iterator<Map.Entry<BigDecimal, LinkedHashMap<String, Order>>> priceItr = lineByPrice.entrySet().iterator();
        MATCHING:
        while (priceItr.hasNext()) {
            Map.Entry<BigDecimal, LinkedHashMap<String, Order>> entry = priceItr.next();
            BigDecimal price = entry.getKey();
            LinkedHashMap<String, Order> orders = entry.getValue();

            // check whether there is price crossing between the taker and the maker
            if (!isPriceCrossed(takerOrder, price)) {
                break;
            }

            Iterator<Map.Entry<String, Order>> orderItr = orders.entrySet().iterator();
            while (orderItr.hasNext()) {
                Map.Entry<String, Order> orderEntry = orderItr.next();
                Order makerOrder = orderEntry.getValue();

                // get taker size
                BigDecimal takerSize;
                if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getType() == OrderType.MARKET) {
                    // The market order does not specify a price, so the size of the maker order needs to be
                    // calculated by the price of the maker order
                    takerSize = takerOrder.getFunds().divide(price, 4, RoundingMode.DOWN);
                } else {
                    takerSize = takerOrder.getSize();
                }

                if (takerSize.compareTo(BigDecimal.ZERO) == 0) {
                    break MATCHING;
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
                if (tradeSize.compareTo(BigDecimal.ZERO)==0){
                    throw new RuntimeException("");
                }
                logWriter.add(orderMatchLog(takerOrder, makerOrder, tradeSize, tradeFunds, tradeId.incrementAndGet()));

                Map<String, Account> makerAccounts = accountBook.getAccountsByUserId(makerOrder.getUserId());
                Account makerBaseAccount = makerAccounts.get(baseCurrency);
                Account makerQuoteAccount = makerAccounts.get(quoteCurrency);
                accountBook.exchange(takerBaseAccount, takerQuoteAccount, makerBaseAccount, makerQuoteAccount, takerOrder.getSide(), tradeSize, tradeFunds);

                // if the maker order is fully filled, remove it from the order book.
                if (makerOrder.getSize().compareTo(BigDecimal.ZERO) == 0) {
                    orderItr.remove();
                    logWriter.add(orderDoneLog(makerOrder, OrderDoneLog.DoneReason.FILLED));
                    unholdOrderFunds(makerOrder, makerBaseAccount, makerQuoteAccount);
                }
            }

            // remove line with empty order list
            if (orders.isEmpty()) {
                priceItr.remove();
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,
        // Note: The market order will never be added to the order book, and the market order without fully filled
        // will be cancelled
        if (takerOrder.getType() == OrderType.LIMIT && takerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
            takerPage.addOrder(takerOrder);
            logWriter.add(orderOpenLog(takerOrder));
        } else {
            OrderDoneLog.DoneReason doneReason = takerOrder.getSize().compareTo(BigDecimal.ZERO) > 0
                    ? OrderDoneLog.DoneReason.CANCELLED
                    : OrderDoneLog.DoneReason.FILLED;
            logWriter.add(orderDoneLog(takerOrder, doneReason));
            unholdOrderFunds(takerOrder, takerBaseAccount, takerQuoteAccount);
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
        //unholdOrderFunds(order, product.getBaseCurrency(), product.getQuoteCurrency());
    }

    public void addOrder(Order order) {
        LinkedHashMap<String, Order> orders = lineByPrice.computeIfAbsent(order.getPrice(), k -> new LinkedHashMap<>());
        orders.put(order.getOrderId(), order);
        orderById.put(order.getOrderId(), order);
    }

    public void removeOrderById(String orderId) {
        Order order = orderById.remove(orderId);
        if (order == null) {
            return;
        }

        LinkedHashMap<String, Order> orders = lineByPrice.get(order.getPrice());
        orders.remove(orderId);
        if (orders.isEmpty()) {
            lineByPrice.remove(order.getPrice());
        }
    }

    public Collection<LinkedHashMap<String, Order>> getLines() {
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

    private OrderRejectedLog orderRejectedLog(Order order, RejectReason rejectReason) {
        OrderRejectedLog log = new OrderRejectedLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setUserId(order.getUserId());
        log.setPrice(order.getPrice());
        log.setFunds(order.getFunds());
        log.setSide(order.getSide());
        log.setSize(order.getSize());
        log.setOrderId(order.getOrderId());
        log.setOrderType(order.getType());
        log.setTime(new Date());
        log.setRejectReason(rejectReason);
        return log;
    }

    private OrderReceivedLog orderReceivedLog(Order order) {
        OrderMessage orderMessage=new OrderMessage();
        orderMessage.setProductId(productId);
        orderMessage.setUserId(order.getUserId());
        orderMessage.setPrice(order.getPrice());
        orderMessage.setFunds(order.getFunds());
        orderMessage.setSide(order.getSide());
        orderMessage.setSize(order.getSize());
        orderMessage.setOrderId(order.getOrderId());
       // orderMessage.setType(order.getType());
        orderMessage.setTime(new Date());
        logWriter.add(orderMessage);

        OrderReceivedLog log = new OrderReceivedLog();
        log.setSequence(sequence.incrementAndGet());
        log.setProductId(productId);
        log.setUserId(order.getUserId());
        log.setPrice(order.getPrice());
        log.setFunds(order.getFunds());
        log.setSide(order.getSide());
        log.setSize(order.getSize());
        log.setOrderId(order.getOrderId());
        log.setOrderType(order.getType());
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

    private OrderMatchLog orderMatchLog(Order takerOrder, Order makerOrder, BigDecimal size, BigDecimal funds, long tradeId) {
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
        log.setFunds(funds);
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
