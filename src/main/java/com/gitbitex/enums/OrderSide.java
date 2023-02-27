package com.gitbitex.enums;

public enum OrderSide {
    /**
     * buy
     */
    BUY,
    /**
     * sell
     */
    SELL;

    /**
     * opposite
     *
     * @return
     */
    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}
