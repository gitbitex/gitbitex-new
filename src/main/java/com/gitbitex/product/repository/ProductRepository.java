package com.gitbitex.product.repository;

import com.gitbitex.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, CrudRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    Product findByProductId(String productId);
}
