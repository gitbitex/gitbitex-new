package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookState {
    private String productId;
    private long orderSequence;
    private long tradeSequence;
    private long messageSequence;
}
