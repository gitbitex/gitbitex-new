package com.gitbitex.matchingengine;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookSnapshot {
    private String productId;
    private long tradeId;
    private List<BookOrder> asks;
    private List<BookOrder> bids;
}
