package com.gitbitex.marketdata.repository;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.marketdata.entity.OrderEntity;
import com.gitbitex.openapi.model.PagedList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class OrderRepository {
    private final MongoCollection<OrderEntity> collection;

    public OrderRepository(MongoDatabase database) {
        this.collection = database.getCollection(OrderEntity.class.getSimpleName().toLowerCase(), OrderEntity.class);
        this.collection.createIndex(Indexes.descending("userId", "productId", "sequence"));
    }

    public OrderEntity findByOrderId(String orderId) {
        return this.collection
                .find(Filters.eq("_id", orderId))
                .first();
    }

    public PagedList<OrderEntity> findAll(String userId, String productId, OrderStatus status, OrderSide side, int pageIndex,
                                          int pageSize) {
        Bson filter = Filters.empty();
        if (userId != null) {
            filter = Filters.and(Filters.eq("userId", userId), filter);
        }
        if (productId != null) {
            filter = Filters.and(Filters.eq("productId", productId), filter);
        }
        if (status != null) {
            filter = Filters.and(Filters.eq("status", status.name()), filter);
        }
        if (side != null) {
            filter = Filters.and(Filters.eq("side", side.name()), filter);
        }

        long count = this.collection.countDocuments(filter);
        List<OrderEntity> orders = this.collection
                .find(filter)
                .sort(Sorts.descending("sequence"))
                .skip(pageIndex - 1)
                .limit(pageSize)
                .into(new ArrayList<>());
        return new PagedList<>(orders, count);
    }

    public void saveAll(Collection<OrderEntity> orders) {
        List<WriteModel<OrderEntity>> writeModels = new ArrayList<>();
        for (OrderEntity item : orders) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<OrderEntity> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }
}
