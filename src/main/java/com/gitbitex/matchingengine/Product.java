package com.gitbitex.matchingengine;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product implements Cloneable {
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    @Override
    public Product clone() {
        try {
            return (Product)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
