package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderStatus;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class EngineSnapshotStore {
    private final MongoCollection<EngineState> engineStateCollection;
    private final MongoCollection<Account> accountCollection;
    private final MongoCollection<Order> orderCollection;
    private final MongoCollection<Product> productCollection;
    private final MongoCollection<OrderBookState> orderBookStateCollection;
    private final MongoClient mongoClient;

    public EngineSnapshotStore(MongoClient mongoClient, MongoDatabase database) {
        this.mongoClient = mongoClient;
        this.engineStateCollection = database.getCollection("snapshot_engine", EngineState.class);
        this.accountCollection = database.getCollection("snapshot_account", Account.class);
        this.orderCollection = database.getCollection("snapshot_order", Order.class);
        this.orderCollection.createIndex(Indexes.descending("product_id", "sequence"), new IndexOptions().unique(true));
        this.productCollection = database.getCollection("snapshot_product", Product.class);
        this.orderBookStateCollection = database.getCollection("snapshot_order_book", OrderBookState.class);
        this.orderBookStateCollection.createIndex(Indexes.descending("product_id"), new IndexOptions().unique(true));
    }

    public List<Product> getProducts() {
        return this.productCollection
                .find()
                .into(new ArrayList<>());
    }

    public List<Account> getAccounts() {
        return this.accountCollection
                .find()
                .into(new ArrayList<>());
    }

    public List<Order> getOrders(String productId) {
        return this.orderCollection
                .find(Filters.eq("productId", productId))
                .sort(Sorts.ascending("sequence"))
                .into(new ArrayList<>());
    }

    public List<OrderBookState> getOrderBookStates() {
        return orderBookStateCollection
                .find()
                .into(new ArrayList<>());
    }

    public EngineState getEngineState() {
        return engineStateCollection
                .find(Filters.eq("_id", "default"))
                .first();
    }

    public Long getCommandOffset() {
        EngineState engineState = getEngineState();
        return engineState != null ? engineState.getCommandOffset() : null;
    }

    public void save(Long commandOffset, OrderBookState orderBookState, Collection<Account> accounts,
                     Collection<Order> orders, Collection<Product> products) {
        EngineState engineState = new EngineState();
        engineState.setCommandOffset(commandOffset);
        List<WriteModel<Account>> accountWriteModels = buildAccountWriteModels(accounts);
        List<WriteModel<Product>> productWriteModels = buildProductWriteModels(products);
        List<WriteModel<Order>> orderWriteModels = buildOrderWriteModels(orders);
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            try {
                engineStateCollection.replaceOne(session, Filters.eq("_id", engineState.getId()), engineState,
                        new ReplaceOptions().upsert(true));
                if (orderBookState != null) {
                    orderBookStateCollection.replaceOne(session,
                            Filters.eq("productId", orderBookState.getProductId()),
                            orderBookState, new ReplaceOptions().upsert(true));
                }
                if (!accountWriteModels.isEmpty()) {
                    accountCollection.bulkWrite(session, accountWriteModels, new BulkWriteOptions().ordered(false));
                }
                if (!productWriteModels.isEmpty()) {
                    productCollection.bulkWrite(session, productWriteModels, new BulkWriteOptions().ordered(false));
                }
                if (!orderWriteModels.isEmpty()) {
                    orderCollection.bulkWrite(session, orderWriteModels, new BulkWriteOptions().ordered(false));
                }
                session.commitTransaction();
            } catch (Exception e) {
                session.abortTransaction();
                throw new RuntimeException(e);
            }
        }
    }

    private List<WriteModel<Product>> buildProductWriteModels(Collection<Product> products) {
        List<WriteModel<Product>> writeModels = new ArrayList<>();
        if (products.isEmpty()) {
            return writeModels;
        }
        for (Product item : products) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Product> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        return writeModels;
    }

    private List<WriteModel<Order>> buildOrderWriteModels(Collection<Order> orders) {
        List<WriteModel<Order>> writeModels = new ArrayList<>();
        if (orders.isEmpty()) {
            return writeModels;
        }
        for (Order item : orders) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Order> writeModel;
            if (item.getStatus() == OrderStatus.OPEN) {
                writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            } else {
                writeModel = new DeleteOneModel<>(filter);
            }
            writeModels.add(writeModel);
        }
        return writeModels;
    }

    private List<WriteModel<Account>> buildAccountWriteModels(Collection<Account> accounts) {
        List<WriteModel<Account>> writeModels = new ArrayList<>();
        if (accounts.isEmpty()) {
            return writeModels;
        }
        for (Account item : accounts) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Account> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        return writeModels;
    }

}
