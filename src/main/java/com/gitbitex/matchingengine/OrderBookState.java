package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderBookState {
    private String id;
    private Long tradeId;
    private Long sequence;
}
