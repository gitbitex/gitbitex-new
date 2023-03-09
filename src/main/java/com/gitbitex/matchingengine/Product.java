package com.gitbitex.matchingengine;

import java.util.Objects;

import com.gitbitex.matchingengine.command.PutProductCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Product implements Cloneable {
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    public Product() {}

    public Product(PutProductCommand command) {
        this.productId = command.getProductId();
        this.baseCurrency = command.getBaseCurrency();
        this.quoteCurrency = command.getQuoteCurrency();
    }

    @Override
    public Product clone() {
        try {
            return (Product)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Product)) {
            return false;
        }
        Product other = (Product)obj;
        return Objects.equals(this.productId, other.productId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (productId == null ? 0 : productId.hashCode());
        return result;
    }
}
