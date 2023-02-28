package com.gitbitex.marketdata.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gitbitex.marketdata.entity.Trade;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.WriteModel;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

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
