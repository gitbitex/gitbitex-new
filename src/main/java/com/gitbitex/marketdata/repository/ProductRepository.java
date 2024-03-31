package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.ProductEntity;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductRepository {
    private final MongoCollection<ProductEntity> mongoCollection;

    public ProductRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(ProductEntity.class.getSimpleName().toLowerCase(), ProductEntity.class);
    }

    public ProductEntity findById(String id) {
        return this.mongoCollection.find(Filters.eq("_id", id)).first();
    }

    public List<ProductEntity> findAll() {
        return this.mongoCollection.find().into(new ArrayList<>());
    }

    public void save(ProductEntity product) {
        List<WriteModel<ProductEntity>> writeModels = new ArrayList<>();
        Bson filter = Filters.eq("_id", product.getId());
        WriteModel<ProductEntity> writeModel = new ReplaceOneModel<>(filter, product, new ReplaceOptions().upsert(true));
        writeModels.add(writeModel);
        this.mongoCollection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }
}
