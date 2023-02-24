package com.gitbitex.matchingengine.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gitbitex.matchingengine.AccountBook.Account;
import com.gitbitex.matchingengine.BookOrder;
import com.gitbitex.matchingengine.OrderBook;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class FullOrderBookSnapshot implements Serializable {
    private String productId;
    private long tradeId;
    private long sequence;
    private List<String> orderIds = new ArrayList<>();
    private long commandOffset;
    private long logOffset;
    private boolean stable;
    private long time;
    private List<BookOrder> asks = new ArrayList<>();
    private List<BookOrder> bids = new ArrayList<>();

    public FullOrderBookSnapshot() {
    }

    public FullOrderBookSnapshot(OrderBook orderBook) {
        this.productId = orderBook.getProductId();
        this.tradeId = orderBook.getTradeId().get();
        this.sequence = orderBook.getSequence().get();
        this.commandOffset = orderBook.getCommandOffset();
        this.logOffset = orderBook.getLogOffset();
        this.stable = orderBook.isStable();
        this.time = System.currentTimeMillis();
        //this.orderIds.addAll(orderBook.getOrderIds());
        orderBook.getAsks().getOrders().forEach(x -> {
            BookOrder order = new BookOrder();
            BeanUtils.copyProperties(x, order);
            this.asks.add(order);
        });
        orderBook.getBids().getOrders().forEach(x -> {
            BookOrder order = new BookOrder();
            BeanUtils.copyProperties(x, order);
            this.bids.add(order);
        });
    }
}
