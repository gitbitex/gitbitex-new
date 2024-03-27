package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.message.OrderMessage;
import com.gitbitex.matchingengine.message.TradeMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Slf4j
public class OrderBook {
    private final String productId;
    private final ProductBook productBook;
    private final AccountBook accountBook;
    private final Depth asks = new Depth(Comparator.naturalOrder());
    private final Depth bids = new Depth(Comparator.reverseOrder());
    private final Map<String, Order> orderById = new HashMap<>();
    private final MessageSender messageSender;
    private final AtomicLong messageSequence;
    private long orderSequence;
    private long tradeSequence;

    public OrderBook(String productId,
                     long orderSequence, long tradeSequence,
                     AccountBook accountBook, ProductBook productBook, MessageSender messageSender, AtomicLong messageSequence) {
        this.productId = productId;
        this.productBook = productBook;
        this.accountBook = accountBook;
        this.orderSequence = orderSequence;
        this.tradeSequence = tradeSequence;
        this.messageSender = messageSender;
        this.messageSequence = messageSequence;
    }

    public void placeOrder(Order takerOrder) {
        var product = productBook.getProduct(productId);
        if (product == null) {
            logger.warn("order rejected, reason: PRODUCT_NOT_FOUND");
            return;
        }

        takerOrder.setSequence(++orderSequence);

        boolean ok;
        if (takerOrder.getSide() == OrderSide.BUY) {
            ok = accountBook.hold(takerOrder.getUserId(), product.getQuoteCurrency(), takerOrder.getRemainingFunds());
        } else {
            ok = accountBook.hold(takerOrder.getUserId(), product.getBaseCurrency(), takerOrder.getRemainingSize());
        }
        if (!ok) {
            logger.warn("order rejected, reason: INSUFFICIENT_FUNDS: {}", JSON.toJSONString(takerOrder));
            takerOrder.setStatus(OrderStatus.REJECTED);
            messageSender.send(orderMessage(takerOrder.clone()));
            return;
        }

        // order received
        takerOrder.setStatus(OrderStatus.RECEIVED);
        messageSender.send(orderMessage(takerOrder.clone()));

        // start matching
        var makerDepth = takerOrder.getSide() == OrderSide.BUY ? asks : bids;
        var depthEntryItr = makerDepth.entrySet().iterator();
        MATCHING:
        while (depthEntryItr.hasNext()) {
            var entry = depthEntryItr.next();
            var price = entry.getKey();
            var orders = entry.getValue();

            // check whether there is price crossing between the taker and the maker
            if (!isPriceCrossed(takerOrder, price)) {
                break;
            }

            var orderItr = orders.entrySet().iterator();
            while (orderItr.hasNext()) {
                var orderEntry = orderItr.next();
                var makerOrder = orderEntry.getValue();

                // make trade
                Trade trade = trade(takerOrder, makerOrder);
                if (trade == null) {
                    break MATCHING;
                }

                // exchange account funds
                accountBook.exchange(takerOrder.getUserId(), makerOrder.getUserId(), product.getBaseCurrency(),
                        product.getQuoteCurrency(), takerOrder.getSide(), trade.getSize(), trade.getFunds());

                // if the maker order is filled or cancelled, remove it from the order book.
                if (makerOrder.getStatus() == OrderStatus.FILLED || makerOrder.getStatus() == OrderStatus.CANCELLED) {
                    orderItr.remove();
                    orderById.remove(makerOrder.getId());
                    messageSender.send(orderMessage(makerOrder.clone()));
                    unholdOrderFunds(makerOrder, product);
                }

                messageSender.send(orderMessage(makerOrder.clone()));
                messageSender.send(tradeMessage(trade));
            }

            // remove price line with empty order list
            if (orders.isEmpty()) {
                depthEntryItr.remove();
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,The market order will never be added to the order book, and the market order without
        // fully filled will be cancelled
        if (takerOrder.getType() == OrderType.LIMIT && takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
            addOrder(takerOrder);
            takerOrder.setStatus(OrderStatus.OPEN);
        } else {
            if (takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                takerOrder.setStatus(OrderStatus.CANCELLED);
            } else {
                takerOrder.setStatus(OrderStatus.FILLED);
            }
            unholdOrderFunds(takerOrder, product);
        }

        messageSender.send(orderMessage(takerOrder.clone()));
    }

    public void cancelOrder(String orderId) {
        var order = orderById.remove(orderId);
        if (order == null) {
            return;
        }

        // remove order from depth
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.removeOrder(order);

        order.setStatus(OrderStatus.CANCELLED);

        messageSender.send(orderMessage(order.clone()));

        // un-hold funds
        var product = productBook.getProduct(productId);
        unholdOrderFunds(order, product);
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
        trade.setSequence(++tradeSequence);
        trade.setProductId(productId);
        trade.setSize(tradeSize);
        trade.setFunds(tradeFunds);
        trade.setPrice(price);
        trade.setSide(makerOrder.getSide());
        trade.setTime(takerOrder.getTime());
        trade.setTakerOrderId(takerOrder.getId());
        trade.setMakerOrderId(makerOrder.getId());
        return trade;
    }

    public void addOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.addOrder(order);
        orderById.put(order.getId(), order);
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

    private void unholdOrderFunds(Order makerOrder, Product product) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getRemainingFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getQuoteCurrency(), makerOrder.getRemainingFunds());
            }
        } else {
            if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getBaseCurrency(), makerOrder.getRemainingSize());
            }
        }
    }


    private OrderMessage orderMessage(Order order) {
        OrderMessage message = new OrderMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setOrder(order);
        return message;
    }

    private TradeMessage tradeMessage(Trade trade) {
        TradeMessage message = new TradeMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setTrade(trade);
        return message;
    }
}
