package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.PutProductCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product implements Cloneable {
    private String id;
    private String baseCurrency;
    private String quoteCurrency;

    public Product() {
    }

    public Product(PutProductCommand command) {
        this.id = command.getProductId();
        this.baseCurrency = command.getBaseCurrency();
        this.quoteCurrency = command.getQuoteCurrency();
    }

    @Override
    public Product clone() {
        try {
            return (Product) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
