package com.gitbitex.matchingengine;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class ProductBook {
    @Getter
    private final Map<String, Product> products = new HashMap<>();

    public ProductBook(){
        Product product = new Product();
        product.setProductId("BTC-USDT");
        product.setBaseCurrency("BTC");
        product.setQuoteCurrency("USDT");
        this.products.put(product.getProductId(),product);
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }
}
