package com.gitbitex.product;

import java.util.concurrent.TimeUnit;

import com.gitbitex.product.entity.Product;
import com.gitbitex.product.repository.ProductRepository;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductRepository productRepository;
    private final LoadingCache<String, Product> productByIdCache
        = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Product>() {
            @Override
            public Product load(String s) throws Exception {
                return productRepository.findByProductId(s);
            }
        });

    @SneakyThrows
    public Product getProductById(String productId) {
        return productByIdCache.get(productId);
    }
}
