package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Trade;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class TradeRepository {
    private final MongoCollection<Trade> collection;

    public TradeRepository(MongoDatabase database) {
        this.collection = database.getCollection(Trade.class.getSimpleName().toLowerCase(), Trade.class);
        this.collection.createIndex(Indexes.descending("productId", "sequence"));
    }

    public List<Trade> findByProductId(String productId, int limit) {
        return this.collection.find(Filters.eq("productId", productId))
                .sort(Sorts.descending("sequence"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public void saveAll(Collection<Trade> trades) {
        List<WriteModel<Trade>> writeModels = new ArrayList<>();
        for (Trade item : trades) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Trade> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }

}
