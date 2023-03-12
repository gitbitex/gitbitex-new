package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Product;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductRepository {
    private final MongoCollection<Product> mongoCollection;

    public ProductRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Product.class.getSimpleName().toLowerCase(), Product.class);
    }

    public Product findById(String id) {
        return this.mongoCollection.find(Filters.eq("_id", id)).first();
    }

    public List<Product> findAll() {
        return this.mongoCollection.find().into(new ArrayList<>());
    }

    public void save(Product product) {
        List<WriteModel<Product>> writeModels = new ArrayList<>();
        Bson filter = Filters.eq("_id", product.getId());
        WriteModel<Product> writeModel = new ReplaceOneModel<>(filter, product, new ReplaceOptions().upsert(true));
        writeModels.add(writeModel);
        this.mongoCollection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));

    }
}
