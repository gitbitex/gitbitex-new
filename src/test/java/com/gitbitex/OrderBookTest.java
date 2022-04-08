
package com.gitbitex;

import com.alibaba.fastjson.JSON;

import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderStatus;
import com.gitbitex.order.entity.Order.OrderType;


import com.gitbitex.matchingengine.OrderBook;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.matchingengine.log.OrderBookLog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBookTest {
    AtomicLong orderId = new AtomicLong(0);

    @Test
    public void test_full_fill() {
        orderId = new AtomicLong(0);

        OrderBook orderBook = new OrderBook("BTC-USDT");

        for (int i = 1; i <= 1; i++) {
            NewOrderCommand order = newOrder(1, 1, OrderType.LIMIT, OrderSide.BUY);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            messages.forEach(x -> System.out.println(JSON.toJSONString(x)));
        }

        {
            NewOrderCommand order = newOrder(1, 1, OrderType.LIMIT, OrderSide.SELL);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            //Assert.isTrue(orderBook.getOrderById(order.getOrderId()).getSize().compareTo(new BigDecimal("1")) == 0);
            //Assert.isTrue(messages.size() == 1);
            //Assert.isTrue(messages.get(0).getType() == Type.ORDER_OPEN);
            messages.forEach(x -> System.out.println(JSON.toJSONString(x)));
        }
    }

    @Test
    public void test_partly_fill() {
        orderId = new AtomicLong(0);

        OrderBook orderBook = new OrderBook("BTC-USDT");

        for (int i = 1; i <= 1; i++) {
            NewOrderCommand order = newOrder(1, 1, OrderType.LIMIT, OrderSide.BUY);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            messages.forEach(x -> System.out.println(JSON.toJSONString(x)));
        }

        {
            NewOrderCommand order = newOrder(1, 0.5, OrderType.LIMIT, OrderSide.SELL);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            //Assert.isTrue(orderBook.getOrderById(order.getOrderId()).getSize().compareTo(new BigDecimal("1")) == 0);
            //Assert.isTrue(messages.size() == 1);
            //Assert.isTrue(messages.get(0).getType() == Type.ORDER_OPEN);
            messages.forEach(x -> System.out.println(JSON.toJSONString(x)));
        }
    }

    @Test
    public void test() {
        orderId = new AtomicLong(0);

        OrderBook orderBook = new OrderBook("BTC-USDT");

        for ( int i=1;i<=1;i++)
        {
            NewOrderCommand order = newOrder(1, 1, OrderType.LIMIT, OrderSide.BUY);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            messages.forEach(x->System.out.println(JSON.toJSONString(x)));
        }



        {
            NewOrderCommand order = newOrder(1, 1, OrderType.LIMIT, OrderSide.SELL);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            //Assert.isTrue(orderBook.getOrderById(order.getOrderId()).getSize().compareTo(new BigDecimal("1")) == 0);
            //Assert.isTrue(messages.size() == 1);
            //Assert.isTrue(messages.get(0).getType() == Type.ORDER_OPEN);
            messages.forEach(x->System.out.println(JSON.toJSONString(x)));
        }

        if (true)return;


        {
            NewOrderCommand order = newOrder(1, 2, OrderType.MARKET, OrderSide.BUY);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
            //Assert.isTrue(orderBook.getOrderById(order.getOrderId()).getSize().compareTo(new BigDecimal("1")) == 0);
            //Assert.isTrue(messages.size() == 1);
            //Assert.isTrue(messages.get(0).getType() == Type.ORDER_OPEN);
            messages.forEach(x->System.out.println(JSON.toJSONString(x)));
        }

        if (true)return;

        {
            NewOrderCommand order = newOrder(1, 0.5, OrderType.LIMIT, OrderSide.SELL);
            List<OrderBookLog> messages = orderBook.executeCommand(order);

        }

        {
            NewOrderCommand order = newOrder(1, 2, OrderType.MARKET, OrderSide.BUY);
            List<OrderBookLog> messages = orderBook.executeCommand(order);
        }


        System.out.println(JSON.toJSONString(orderBook, true));

    }

    private NewOrderCommand newOrder(double price, double size, OrderType orderType, OrderSide orderSide) {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        order.setOrderId(String.valueOf(orderId.incrementAndGet()));
        order.setPrice(BigDecimal.valueOf(price));
        order.setSize(BigDecimal.valueOf(size));
        order.setFunds(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(size)).setScale(2, RoundingMode.DOWN));
        order.setType(orderType);
        order.setSide(orderSide);

        NewOrderCommand newOrderCommand=new NewOrderCommand();
        newOrderCommand.setProductId(order.getProductId());
        newOrderCommand.setOrder(order);
        return newOrderCommand;
    }
}

