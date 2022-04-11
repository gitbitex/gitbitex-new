package com.gitbitex.matchingengine.snapshot;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class L2OrderBookUpdate {
    private String productId;
    private Collection<L2PageLineChange> changes;
}
