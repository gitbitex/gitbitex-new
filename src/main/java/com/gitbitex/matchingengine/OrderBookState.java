package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookState {
    private String productId;
    private Long orderSequence;
    private Long tradeSequence;
    private Long messageSequence;
}
