package com.gitbitex.marketdata.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.openapi.model.PagedList;
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
public class CandleRepository {
    private final MongoCollection<Candle> mongoCollection;

    public CandleRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Candle.class.getSimpleName(), Candle.class);
    }

    public Candle findById(String id) {
        return this.mongoCollection.find(Filters.eq("_id", id)).first();
    }

    public PagedList<Candle> findAll(String productId, Integer granularity, int pageIndex, int pageSize) {
        Bson filter = Filters.empty();
        if (productId != null) {
            filter = Filters.and(Filters.eq("productId", productId));
        }
        if (granularity != null) {
            filter = Filters.and(Filters.eq("granularity", granularity));
        }

        long count = this.mongoCollection.countDocuments(filter);
        List<Candle> candles = this.mongoCollection.find(filter)
            .sort(Sorts.descending("time"))
            .skip(pageIndex - 1)
            .limit(pageSize)
            .into(new ArrayList<>());
        return new PagedList<>(candles, count);
    }

    public void saveAll(Collection<Candle> candles) {
        List<WriteModel<Candle>> writeModels = new ArrayList<>();
        for (Candle item : candles) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Candle> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        this.mongoCollection.bulkWrite(writeModels);
    }
}

