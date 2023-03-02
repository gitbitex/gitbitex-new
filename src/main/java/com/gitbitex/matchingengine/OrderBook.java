package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class OrderBook {
    private final String productId;
    private final AtomicLong tradeId = new AtomicLong();
    private final AtomicLong logSequence = new AtomicLong();
    private final LogWriter logWriter;
    private final ProductBook productBook;
    private final AccountBook accountBook;
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> asks = new TreeMap<>(Comparator.naturalOrder());
    private final TreeMap<BigDecimal, PriceGroupOrderCollection> bids = new TreeMap<>(Comparator.reverseOrder());
    private final LinkedHashMap<String, Order> orderById = new LinkedHashMap<>();

    public OrderBook(String productId, LogWriter logWriter, AccountBook accountBook, ProductBook productBook) {
        this.productId = productId;
        this.logWriter = logWriter;
        this.productBook = productBook;
        this.accountBook = accountBook;
    }

    public OrderBook(String productId, OrderBookSnapshot snapshot, LogWriter logWriter, AccountBook accountBook,
        ProductBook productBook) {
        this(productId, logWriter, accountBook, productBook);
        if (snapshot != null) {
            this.tradeId.set(snapshot.getTradeId());
            this.logSequence.set(snapshot.getLogSequence());
            this.addOrders(snapshot.getAsks());
            this.addOrders(snapshot.getBids());
        }
    }

    public void placeOrder(Order takerOrder, Long commandOffset) {
        Product product = productBook.getProduct(productId);
        Account takerBaseAccount = accountBook.getAccount(takerOrder.getUserId(), product.getBaseCurrency());
        Account takerQuoteAccount = accountBook.getAccount(takerOrder.getUserId(), product.getQuoteCurrency());
        List<Object> dirtyObjects = new ArrayList<>();

        dirtyObjects.add(takerOrder);

        if (!holdOrderFunds(takerOrder, takerBaseAccount, takerQuoteAccount)) {
            takerOrder.setStatus(OrderStatus.REJECTED);
            flush(commandOffset, dirtyObjects);
            return;
        }

        // order received
        takerOrder.setStatus(OrderStatus.RECEIVED);
        if (logWriter != null) {
            logWriter.onOrderReceived(takerOrder.clone(), logSequence.incrementAndGet());
        }

        // start matching
        Iterator<Entry<BigDecimal, PriceGroupOrderCollection>> priceItr = (takerOrder.getSide() == OrderSide.BUY
            ? asks : bids).entrySet().iterator();
        MATCHING:
        while (priceItr.hasNext()) {
            Map.Entry<BigDecimal, PriceGroupOrderCollection> entry = priceItr.next();
            BigDecimal price = entry.getKey();
            PriceGroupOrderCollection orders = entry.getValue();

            // check whether there is price crossing between the taker and the maker
            if (!isPriceCrossed(takerOrder, price)) {
                break;
            }

            Iterator<Map.Entry<String, Order>> orderItr = orders.entrySet().iterator();
            while (orderItr.hasNext()) {
                Map.Entry<String, Order> orderEntry = orderItr.next();
                Order makerOrder = orderEntry.getValue();

                // make trade
                Trade trade = trade(takerOrder, makerOrder);
                if (trade == null) {
                    break MATCHING;
                }
                entry.getValue().decrRemainingSize(trade.getSize());

                if (logWriter != null) {
                    logWriter.onOrderMatch(takerOrder.clone(), makerOrder.clone(), trade,
                        logSequence.incrementAndGet());
                }

                // exchange account funds
                Account makerBaseAccount = accountBook.getAccount(makerOrder.getUserId(), product.getBaseCurrency());
                Account makerQuoteAccount = accountBook.getAccount(makerOrder.getUserId(), product.getQuoteCurrency());
                exchange(takerBaseAccount, takerQuoteAccount, makerBaseAccount, makerQuoteAccount, trade);

                // if the maker order is filled or cancelled, remove it from the order book.
                if (makerOrder.getStatus() == OrderStatus.FILLED || makerOrder.getStatus() == OrderStatus.CANCELLED) {
                    orderItr.remove();
                    orderById.remove(makerOrder.getOrderId());
                    if (logWriter != null) {
                        logWriter.onOrderDone(makerOrder.clone(), logSequence.incrementAndGet());
                    }
                    unholdOrderFunds(makerOrder, makerBaseAccount, makerQuoteAccount);
                }

                dirtyObjects.add(makerOrder);
                dirtyObjects.add(makerBaseAccount);
                dirtyObjects.add(makerQuoteAccount);
                dirtyObjects.add(trade);
            }

            // remove price line with empty order list
            if (orders.isEmpty()) {
                priceItr.remove();
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,The market order will never be added to the order book, and the market order without
        // fully filled will be cancelled
        if (takerOrder.getType() == OrderType.LIMIT && takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
            addOrder(takerOrder);
            takerOrder.setStatus(OrderStatus.OPEN);
            if (logWriter != null) {
                logWriter.onOrderOpen(takerOrder.clone(), logSequence.incrementAndGet());
            }
        } else {
            takerOrder.setStatus(OrderStatus.CANCELLED);
            if (logWriter != null) {
                logWriter.onOrderDone(takerOrder.clone(), logSequence.incrementAndGet());
            }
            unholdOrderFunds(takerOrder, takerBaseAccount, takerQuoteAccount);
        }

        dirtyObjects.add(takerBaseAccount);
        dirtyObjects.add(takerQuoteAccount);

        flush(commandOffset, dirtyObjects);
    }

    public void cancelOrder(String orderId, Long commandOffset) {
        Order order = orderById.remove(orderId);
        if (order == null) {
            return;
        }

        // remove order from order book
        TreeMap<BigDecimal, PriceGroupOrderCollection> ordersByPrice = order.getSide() == OrderSide.BUY ? bids : asks;
        LinkedHashMap<String, Order> orders = ordersByPrice.get(order.getPrice());
        orders.remove(orderId);
        if (orders.isEmpty()) {
            ordersByPrice.remove(order.getPrice());
        }
        orderById.remove(orderId);

        order.setStatus(OrderStatus.CANCELLED);
        if (logWriter != null) {
            logWriter.onOrderDone(order.clone(), logSequence.incrementAndGet());
        }

        // unhold funds
        Product product = productBook.getProduct(productId);
        Map<String, Account> makerAccounts = accountBook.getAccountsByUserId(order.getUserId());
        Account makerBaseAccount = makerAccounts.get(product.getBaseCurrency());
        Account makerQuoteAccount = makerAccounts.get(product.getQuoteCurrency());
        unholdOrderFunds(order, makerBaseAccount, makerQuoteAccount);
    }

    private Trade trade(Order takerOrder, Order makerOrder) {
        BigDecimal price = makerOrder.getPrice();

        // get taker size
        BigDecimal takerSize;
        if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getType() == OrderType.MARKET) {
            // The market order does not specify a price, so the size of the maker order needs to be
            // calculated by the price of the maker order
            takerSize = takerOrder.getRemainingFunds().divide(price, 4, RoundingMode.DOWN);
        } else {
            takerSize = takerOrder.getRemainingSize();
        }

        if (takerSize.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // take the minimum size of taker and maker as trade size
        BigDecimal tradeSize = takerSize.min(makerOrder.getRemainingSize());
        BigDecimal tradeFunds = tradeSize.multiply(price);

        // fill order
        takerOrder.setRemainingSize(takerOrder.getRemainingSize().subtract(tradeSize));
        makerOrder.setRemainingSize(makerOrder.getRemainingSize().subtract(tradeSize));
        if (takerOrder.getSide() == OrderSide.BUY) {
            takerOrder.setRemainingFunds(takerOrder.getRemainingFunds().subtract(tradeFunds));
        } else {
            makerOrder.setRemainingFunds(makerOrder.getRemainingFunds().subtract(tradeFunds));
        }
        if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) == 0) {
            makerOrder.setStatus(OrderStatus.FILLED);
        }

        Trade trade = new Trade();
        trade.setTradeId(tradeId.incrementAndGet());
        trade.setProductId(productId);
        trade.setSize(tradeSize);
        trade.setFunds(tradeFunds);
        trade.setPrice(price);
        trade.setSide(makerOrder.getSide());
        trade.setTime(takerOrder.getTime());
        trade.setTakerOrderId(takerOrder.getOrderId());
        trade.setMakerOrderId(makerOrder.getOrderId());
        return trade;
    }

    private void exchange(Account takerBaseAccount, Account takerQuoteAccount, Account makerBaseAccount,
        Account makerQuoteAccount, Trade trade) {
        accountBook.exchange(takerBaseAccount, takerQuoteAccount, makerBaseAccount, makerQuoteAccount, trade.getSide(),
            trade.getSize(), trade.getFunds());
    }

    private void flush(Long commandOffset, List<Object> dirtyObjects) {
        if (logWriter != null) {
            logWriter.flush(commandOffset, dirtyObjects);
        }
    }

    public void addOrder(Order order) {
        (order.getSide() == OrderSide.BUY ? bids : asks)
            .computeIfAbsent(order.getPrice(), k -> new PriceGroupOrderCollection())
            .put(order.getOrderId(), order);
        orderById.put(order.getOrderId(), order);
    }

    public void addOrders(List<Order> orders) {
        if (orders != null) {
            orders.forEach(this::addOrder);
        }
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

    private boolean holdOrderFunds(Order takerOrder, Account takerBaseAccount, Account takerQuoteAccount) {
        if (takerOrder.getSide() == OrderSide.BUY) {
            if (takerQuoteAccount == null || takerQuoteAccount.getAvailable().compareTo(takerOrder.getRemainingFunds())
                < 0) {
                return false;
            }
            accountBook.hold(takerQuoteAccount, takerOrder.getRemainingFunds());
        } else {
            if (takerBaseAccount == null || takerBaseAccount.getAvailable().compareTo(takerOrder.getRemainingSize())
                < 0) {
                return false;
            }
            accountBook.hold(takerBaseAccount, takerOrder.getRemainingSize());
        }
        return true;
    }

    private void unholdOrderFunds(Order makerOrder, Account baseAccount, Account quoteAccount) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getRemainingFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(quoteAccount, makerOrder.getRemainingFunds());
            }
        } else {
            if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(baseAccount, makerOrder.getRemainingSize());
            }
        }
    }
}
