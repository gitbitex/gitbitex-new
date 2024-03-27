package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.message.ProductMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ProductBook {
    private final Map<String, Product> products = new HashMap<>();
    private final MessageSender messageSender;
    private final AtomicLong messageSequence;

    public Collection<Product> getAllProducts() {
        return products.values();
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    public void putProduct(Product product) {
        this.products.put(product.getId(), product);
        messageSender.send(productMessage(product.clone()));
    }

    public void addProduct(Product product) {
        this.products.put(product.getId(), product);
    }

    private ProductMessage productMessage(Product product) {
        ProductMessage message = new ProductMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setProduct(product);
        return message;
    }
}
