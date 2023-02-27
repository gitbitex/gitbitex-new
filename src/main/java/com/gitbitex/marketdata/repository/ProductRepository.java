package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends MongoRepository<Product, Long>, CrudRepository<Product, Long> {

    Product findByProductId(String productId);
}
