package com.gitbitex.marketdata.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.openapi.model.PagedList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

@Component
public class OrderRepository {
    private final MongoCollection<Order> mongoCollection;

    public OrderRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Order.class.getSimpleName(), Order.class);
    }

    public Order findByOrderId(String orderId) {
        return this.mongoCollection.find(Filters.eq("orderId", orderId)).first();
    }

    public PagedList<Order> findAll(String userId, String productId, OrderStatus status, OrderSide side, int pageIndex,
        int pageSize) {
        Bson filter = Filters.empty();
        if (userId != null) {
            filter = Filters.and(Filters.eq("userId", userId));
        }
        if (productId != null) {
            filter = Filters.and(Filters.eq("productId", productId));
        }
        if (status != null) {
            filter = Filters.and(Filters.eq("status", status.name()));
        }
        if (side != null) {
            filter = Filters.and(Filters.eq("side", side.name()));
        }

        long count = this.mongoCollection.countDocuments(filter);
        List<Order> orders = this.mongoCollection.find(filter)
            .skip(pageIndex - 1)
            .limit(pageSize)
            .into(new ArrayList<>());
        return new PagedList<>(orders, count);
    }

    public void saveAll(Collection<Order> orders) {
        List<WriteModel<Order>> writeModels = new ArrayList<>();
        for (Order item : orders) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Order> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        mongoCollection.bulkWrite(writeModels);
    }
}
