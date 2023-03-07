package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.PutProductCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product implements Cloneable {
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    public Product(PutProductCommand command){
        this.productId=command.getProductId();
        this.baseCurrency=command.getBaseCurrency();
        this.quoteCurrency=command.getQuoteCurrency();
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
