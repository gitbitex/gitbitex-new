package com.gitbitex.matchingengine;

import java.util.HashMap;
import java.util.Map;

public class ProductBook {
    private final Map<String, Product> products = new HashMap<>();

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    public void putProduct(Product product, ModifiedObjectList modifiedObjects) {
        this.products.put(product.getId(), product);
        modifiedObjects.add(product.clone());
    }

    public void addProduct(Product product) {
        this.products.put(product.getId(), product);
    }
}
