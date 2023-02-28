package com.gitbitex.marketdata.repository;

import java.util.ArrayList;
import java.util.List;

import com.gitbitex.marketdata.entity.Product;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.springframework.stereotype.Component;

@Component
public class ProductRepository {
    private final MongoCollection<Product> mongoCollection;

    public ProductRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Product.class.getSimpleName(), Product.class);
    }

    public List<Product> findAll() {
        return this.mongoCollection.find().into(new ArrayList<>());
    }

    public void save(Product product) {
        this.mongoCollection.insertOne(product);
    }
}
