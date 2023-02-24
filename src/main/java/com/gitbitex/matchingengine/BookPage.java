package com.gitbitex.matchingengine;

import java.io.Serializable;
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

import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage.RejectReason;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.entity.Order.OrderSide;
import com.gitbitex.marketdata.entity.Order.OrderType;
import org.springframework.beans.BeanUtils;

public class BookPage implements Serializable {
    private final String productId;
    private final TreeMap<BigDecimal, PageLine> lineByPrice;
    private final LinkedHashMap<String, BookOrder> orderById = new LinkedHashMap<>();
    private final AtomicLong tradeId;
    private final AtomicLong sequence;
    private final LogWriter logWriter;
    private final AccountBook accountBook;

    public BookPage(String productId, Comparator<BigDecimal> priceComparator, AtomicLong tradeId, AtomicLong sequence,
        AccountBook accountBook, LogWriter logWriter) {
        this.lineByPrice = new TreeMap<>(priceComparator);
        this.productId = productId;
        this.tradeId = tradeId;
        this.sequence = sequence;
        this.logWriter = logWriter;
        this.accountBook = accountBook;
    }

    public void executeCommand(PlaceOrderCommand command, BookPage oppositePage) {
        Order order = command.getOrder();
        BookOrder takerOrder = new BookOrder();
        BeanUtils.copyProperties(order, takerOrder);

        String takerUserId = takerOrder.getUserId();
        String baseCurrency = "BTC";
        String quoteCurrency = "USDT";

        if (takerOrder.getSide() == OrderSide.BUY) {
            if (takerOrder.getFunds().compareTo( accountBook.getAvailable(takerUserId,quoteCurrency) ) > 0) {
                logWriter.add(orderRejectedLog(command.getOffset(), order,RejectReason.INSUFFICIENT_BALANCE));
                return;
            } else {
                accountBook.hold(takerUserId, quoteCurrency, takerOrder.getFunds());
            }
        } else {
            if (takerOrder.getSize().compareTo(accountBook.getAvailable(takerUserId,baseCurrency)  ) > 0) {
                logWriter.add(orderRejectedLog(command.getOffset(), order,RejectReason.INSUFFICIENT_BALANCE));
                return;
            } else {
                accountBook.hold(takerUserId, baseCurrency, takerOrder.getSize());
            }
        }

        logWriter.add(orderReceivedLog(command.getOffset(), order));

        /*if (order.getStatus() != Order.OrderStatus.NEW) {
            logs.get(0).setCommandFinished(true);
            return logs;
        }*/

        // If it's a Market-Buy order, set price to infinite high, and if it's market-sell,
        // set price to zero, which ensures that prices will cross.
        if (takerOrder.getType() == Order.OrderType.MARKET) {
            if (takerOrder.getSide() == Order.OrderSide.BUY) {
                takerOrder.setPrice(BigDecimal.valueOf(Long.MAX_VALUE));
            } else {
                takerOrder.setPrice(BigDecimal.ZERO);
            }
        }

        List<BookOrder> makerOrders = new ArrayList<>();

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
                logWriter.add(orderDoneLog(command.getOffset(), takerOrder));
                //logs.get(logs.size() - 1).setCommandFinished(true);
                //return logs;
            }

            for (BookOrder makerOrder : line.getOrders()) {
                String makerUserId = makerOrder.getUserId();

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
                BigDecimal tradeFunds = tradeSize.multiply(price);

                // adjust the size of taker order
                takerOrder.setSize(takerOrder.getSize().subtract(tradeSize));
                takerOrder.setFunds(takerOrder.getFunds().subtract(tradeFunds));

                // only market-buy order use funds
                if (takerOrder.getSide() == Order.OrderSide.BUY && takerOrder.getType() == Order.OrderType.MARKET) {
                    //takerOrder.setFunds(takerOrder.getFunds().subtract(tradeSize.multiply(price)));
                }

                // adjust the size of maker order
                makerOrder.setSize(makerOrder.getSize().subtract(tradeSize));
                makerOrder.setFunds(makerOrder.getFunds().subtract(tradeFunds));

                tradeId.incrementAndGet();

                OrderFilledMessage fillTakerOrderCommand = new OrderFilledMessage();
                fillTakerOrderCommand.setSequence(sequence.incrementAndGet());
                fillTakerOrderCommand.setOrderId(takerOrder.getOrderId());
                fillTakerOrderCommand.setSide(takerOrder.getSide());
                fillTakerOrderCommand.setProductId(productId);
                fillTakerOrderCommand.setSize(tradeSize);
                fillTakerOrderCommand.setPrice(price);
                fillTakerOrderCommand.setFunds(tradeFunds);
                fillTakerOrderCommand.setTradeId(tradeId.get());
                fillTakerOrderCommand.setUserId(takerOrder.getUserId());
                logWriter.add(fillTakerOrderCommand);

                OrderFilledMessage fillMakerOrderCommand = new OrderFilledMessage();
                fillMakerOrderCommand.setSequence(sequence.incrementAndGet());
                fillMakerOrderCommand.setOrderId(makerOrder.getOrderId());
                fillMakerOrderCommand.setSide(makerOrder.getSide());
                fillMakerOrderCommand.setProductId(productId);
                fillMakerOrderCommand.setSize(tradeSize);
                fillMakerOrderCommand.setPrice(price);
                fillMakerOrderCommand.setFunds(tradeFunds);
                fillMakerOrderCommand.setTradeId(tradeId.get());
                fillMakerOrderCommand.setUserId(makerOrder.getUserId());
                logWriter.add(fillMakerOrderCommand);

                // create a new match log
                logWriter.add(orderMatchLog(command.getOffset(), takerOrder, makerOrder, tradeSize));
                accountBook.exchange(takerUserId, makerUserId, baseCurrency, quoteCurrency, takerOrder.getSide(),tradeSize,tradeFunds);

                makerOrders.add(makerOrder);
            }
        }

        // If the taker order is not fully filled, put the taker order into the order book, otherwise mark
        // the order as done,
        // Note: The market order will never be added to the order book, and the market order without fully filled
        // will be cancelled
        if (takerOrder.getType() == Order.OrderType.LIMIT && takerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
            oppositePage.addOrder(takerOrder);
            logWriter.add(orderOpenLog(command.getOffset(), takerOrder));
        } else {
            logWriter.add(orderDoneLog(command.getOffset(), takerOrder));
            doneOrder(takerOrder, baseCurrency, quoteCurrency);
        }

        // Check all maker orders, if the marker order is fully filled, remove it from the order book
        for (BookOrder makerOrder : makerOrders) {
            if (makerOrder.getSize().compareTo(BigDecimal.ZERO) == 0) {
                removeOrderById(makerOrder.getOrderId());
                logWriter.add(orderDoneLog(command.getOffset(), makerOrder));
                doneOrder(makerOrder, baseCurrency, quoteCurrency);
            }
        }
    }

    private void doneOrder(BookOrder makerOrder, String baseCurrency, String quoteCurrency) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            if (makerOrder.getFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), quoteCurrency, makerOrder.getFunds());
            }
        } else {
            if (makerOrder.getSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), baseCurrency, makerOrder.getFunds());
            }
        }
    }

    public void executeCommand(CancelOrderCommand command) {
        String orderId = command.getOrderId();
        BookOrder order = orderById.get(orderId);
        if (order == null) {
            return;
        }

        removeOrderById(orderId);

        logWriter.add(orderDoneLog(command.getOffset(), order));
        doneOrder(order, "BTC", "USDT");
    }

    public PageLine addOrder(BookOrder order) {
        PageLine line = lineByPrice.computeIfAbsent(order.getPrice(),
            k -> new PageLine(order.getPrice(), order.getSide()));
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
        if (line.getOrders().isEmpty()) {
            lineByPrice.remove(order.getPrice());
        }
        return line;
    }

    public Collection<PageLine> getLines() {
        return this.lineByPrice.values();
    }

    public Collection<BookOrder> getOrders() {
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

    private OrderDoneMessage.DoneReason determineDoneReason(BookOrder order) {
        if (order.getType() == OrderType.MARKET && order.getSide() == OrderSide.BUY) {
            if (order.getFunds().compareTo(BigDecimal.ZERO) > 0) {
                return OrderDoneMessage.DoneReason.CANCELLED;
            }
        }

        if (order.getSize().compareTo(BigDecimal.ZERO) > 0) {
            return OrderDoneMessage.DoneReason.CANCELLED;
        }
        return OrderDoneMessage.DoneReason.FILLED;
    }


    private OrderRejectedMessage orderRejectedLog(long commandOffset, Order order, RejectReason rejectReason) {
        OrderRejectedMessage log = new OrderRejectedMessage();
        log.setSequence(sequence.incrementAndGet());
        log.setCommandOffset(commandOffset);
        log.setProductId(productId);
        log.setOrder(order);
        log.setTime(new Date());
        log.setRejectReason(rejectReason);
        return log;
    }


    private OrderReceivedMessage orderReceivedLog(long commandOffset, Order order) {
        OrderReceivedMessage log = new OrderReceivedMessage();
        log.setSequence(sequence.incrementAndGet());
        log.setCommandOffset(commandOffset);
        log.setProductId(productId);
        log.setOrder(order);
        log.setTime(new Date());
        return log;
    }

    private OrderOpenMessage orderOpenLog(long commandOffset, BookOrder takerOrder) {
        OrderOpenMessage log = new OrderOpenMessage();
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
        log.setTradeId(tradeId.get());
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

    private OrderDoneMessage orderDoneLog(long commandOffset, BookOrder order) {
        OrderDoneMessage log = new OrderDoneMessage();
        log.setCommandOffset(commandOffset);
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
        log.setDoneReason(determineDoneReason(order));
        log.setOrderType(order.getType());
        log.setTime(new Date());
        return log;
    }
}
