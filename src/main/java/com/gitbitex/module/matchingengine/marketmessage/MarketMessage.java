package com.gitbitex.module.matchingengine.marketmessage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketMessage {
    private String type;
    private String productId;

    public MarketMessage() {
    }
}
