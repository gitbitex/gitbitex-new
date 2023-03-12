package com.gitbitex.matchingengine;

import java.util.ArrayList;

public class L2OrderBookChange extends ArrayList<Object> {
    public L2OrderBookChange() {
    }

    public L2OrderBookChange(String side, String price, String size) {
        this.add(side);
        this.add(price);
        this.add(size);
    }
}
