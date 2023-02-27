package com.gitbitex.product.repository;

import com.gitbitex.product.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends MongoRepository<Product, Long>, CrudRepository<Product, Long> {

    Product findByProductId(String productId);
}
