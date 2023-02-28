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
    private final MongoCollection<Trade> mongoCollection;

    public TradeRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Trade.class.getSimpleName(), Trade.class);
    }

    public List<Trade> findTradesByProductId(String productId, int limit) {
        return this.mongoCollection.find(Filters.eq("productId", productId))
                .sort(Sorts.descending("time"))
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
        mongoCollection.bulkWrite(writeModels);
    }

}
