package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.enums.OrderType;
import com.gitbitex.matchingengine.message.OrderDoneMessage;
import com.gitbitex.matchingengine.message.OrderMatchMessage;
import com.gitbitex.matchingengine.message.OrderOpenMessage;
import com.gitbitex.matchingengine.message.OrderReceivedMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Slf4j
public class OrderBook {
    private final String productId;
    private final ProductBook productBook;
    private final AccountBook accountBook;
    private final Depth asks = new Depth(Comparator.naturalOrder());
    private final Depth bids = new Depth(Comparator.reverseOrder());
    private final Map<String, Order> orderById = new HashMap<>();
    private long orderSequence;
    private long tradeSequence;
    private long messageSequence;

    public OrderBook(String productId, long orderSequence, long tradeSequence, long messageSequence,
                     AccountBook accountBook, ProductBook productBook) {
        this.productId = productId;
        this.productBook = productBook;
        this.accountBook = accountBook;
        this.orderSequence = orderSequence;
        this.tradeSequence = tradeSequence;
        this.messageSequence = messageSequence;
    }

    public void placeOrder(Order takerOrder, ModifiedObjectList modifiedObjects) {
        var product = productBook.getProduct(productId);
        if (product == null) {
            logger.warn("order rejected, reason: PRODUCT_NOT_FOUND");
            return;
        }

        takerOrder.setSequence(++orderSequence);

        if (takerOrder.getSide() == OrderSide.BUY) {
            accountBook.hold(takerOrder.getUserId(), product.getQuoteCurrency(), takerOrder.getRemainingFunds(),
                    modifiedObjects);
        } else {
            accountBook.hold(takerOrder.getUserId(), product.getBaseCurrency(), takerOrder.getRemainingSize(),
                    modifiedObjects);
        }
        if (modifiedObjects.isEmpty()) {
            logger.warn("order rejected, reason: INSUFFICIENT_FUNDS: {}", JSON.toJSONString(takerOrder));
            takerOrder.setStatus(OrderStatus.REJECTED);
            modifiedObjects.add(takerOrder);
            modifiedObjects.add(orderBookState());
            return;
        }

        // order received
        takerOrder.setStatus(OrderStatus.RECEIVED);
        modifiedObjects.add(orderReceivedMessage(takerOrder));

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

                modifiedObjects.add(orderMatchMessage(takerOrder, makerOrder, trade));

                // exchange account funds
                accountBook.exchange(takerOrder.getUserId(), makerOrder.getUserId(), product.getBaseCurrency(),
                        product.getQuoteCurrency(), takerOrder.getSide(), trade.getSize(), trade.getFunds(),
                        modifiedObjects);

                // if the maker order is filled or cancelled, remove it from the order book.
                if (makerOrder.getStatus() == OrderStatus.FILLED || makerOrder.getStatus() == OrderStatus.CANCELLED) {
                    orderItr.remove();
                    orderById.remove(makerOrder.getId());
                    modifiedObjects.add(orderDoneMessage(makerOrder));
                    unholdOrderFunds(makerOrder, product, modifiedObjects);
                }

                modifiedObjects.add(makerOrder.clone());
                modifiedObjects.add(trade);
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
            modifiedObjects.add(orderOpenMessage(takerOrder));
        } else {
            if (takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                takerOrder.setStatus(OrderStatus.CANCELLED);
            } else {
                takerOrder.setStatus(OrderStatus.FILLED);
            }
            modifiedObjects.add(orderDoneMessage(takerOrder));
            unholdOrderFunds(takerOrder, product, modifiedObjects);
        }
        modifiedObjects.add(takerOrder.clone());
        modifiedObjects.add(orderBookState());
    }

    public void cancelOrder(String orderId, ModifiedObjectList modifiedObjects) {
        var order = orderById.remove(orderId);
        if (order == null) {
            return;
        }

        // remove order from depth
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.removeOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        modifiedObjects.add(order);
        modifiedObjects.add(orderDoneMessage(order));
        modifiedObjects.add(orderBookState());

        // un-hold funds
        var product = productBook.getProduct(productId);
        unholdOrderFunds(order, product, modifiedObjects);
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

    private void unholdOrderFunds(Order makerOrder, Product product, ModifiedObjectList modifiedObjects) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getRemainingFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getQuoteCurrency(), makerOrder.getRemainingFunds(),
                        modifiedObjects);
            }
        } else {
            if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getBaseCurrency(), makerOrder.getRemainingSize(),
                        modifiedObjects);
            }
        }
    }

    public OrderReceivedMessage orderReceivedMessage(Order order) {
        var message = new OrderReceivedMessage();
        message.setSequence(++messageSequence);
        message.setProductId(order.getProductId());
        message.setUserId(order.getUserId());
        message.setPrice(order.getPrice());
        message.setFunds(order.getRemainingFunds());
        message.setSide(order.getSide());
        message.setSize(order.getRemainingSize());
        message.setOrderId(order.getId());
        message.setOrderType(order.getType());
        message.setTime(new Date());
        return message;
    }

    public OrderOpenMessage orderOpenMessage(Order order) {
        var message = new OrderOpenMessage();
        message.setSequence(++messageSequence);
        message.setProductId(order.getProductId());
        message.setRemainingSize(order.getRemainingSize());
        message.setPrice(order.getPrice());
        message.setSide(order.getSide());
        message.setOrderId(order.getId());
        message.setUserId(order.getUserId());
        message.setTime(new Date());
        return message;
    }

    public OrderMatchMessage orderMatchMessage(Order takerOrder, Order makerOrder, Trade trade) {
        var message = new OrderMatchMessage();
        message.setSequence(++messageSequence);
        message.setTradeId(trade.getSequence());
        message.setProductId(trade.getProductId());
        message.setTakerOrderId(takerOrder.getId());
        message.setMakerOrderId(makerOrder.getId());
        message.setTakerUserId(takerOrder.getUserId());
        message.setMakerUserId(makerOrder.getUserId());
        message.setPrice(makerOrder.getPrice());
        message.setSize(trade.getSize());
        message.setFunds(trade.getFunds());
        message.setSide(makerOrder.getSide());
        message.setTime(takerOrder.getTime());
        return message;
    }

    public OrderDoneMessage orderDoneMessage(Order order) {
        var message = new OrderDoneMessage();
        message.setSequence(++messageSequence);
        message.setProductId(order.getProductId());
        if (order.getType() != OrderType.MARKET) {
            message.setRemainingSize(order.getRemainingSize());
            message.setPrice(order.getPrice());
        }
        message.setRemainingFunds(order.getRemainingFunds());
        message.setRemainingSize(order.getRemainingSize());
        message.setSide(order.getSide());
        message.setOrderId(order.getId());
        message.setUserId(order.getUserId());
        message.setDoneReason(order.getStatus().toString());
        message.setOrderType(order.getType());
        message.setTime(new Date());
        return message;
    }

    public OrderBookState orderBookState() {
        var orderBookState = new OrderBookState();
        orderBookState.setProductId(this.productId);
        orderBookState.setOrderSequence(this.orderSequence);
        orderBookState.setTradeSequence(this.tradeSequence);
        orderBookState.setMessageSequence(this.messageSequence);
        return orderBookState;
    }
}
