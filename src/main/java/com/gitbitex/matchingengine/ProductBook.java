package com.gitbitex.matchingengine;

import java.util.HashMap;
import java.util.Map;

public class ProductBook {
    private final Map<String,Product> products=new HashMap<>();

    public Product getProduct(String productId) {
        Product product=new Product();
        product.setProductId("BTC-USDT");
        product.setBaseCurrency("BTC");
        product.setQuoteCurrency("USDT");
        return product;
        //return products.get(productId);
    }
}
