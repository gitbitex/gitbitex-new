package com.gitbitex.matchingengine;

import java.util.*;

import com.gitbitex.enums.OrderStatus;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.WriteModel;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Slf4j
@Component
public class EngineStateStore {
    private final MongoCollection<EngineState> engineStateCollection;
    private final MongoCollection<Account> accountCollection;
    private final MongoCollection<Order> orderCollection;
    private final MongoCollection<Product> productMongoCollection;
    private final MongoCollection<OrderBookState> orderBookStateCollection;
    private final MongoClient mongoClient;

    public EngineStateStore(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase database = mongoClient.getDatabase("gitbitex").withCodecRegistry(pojoCodecRegistry);
        this.engineStateCollection = database.getCollection("engine_state_engine", EngineState.class);
        this.accountCollection = database.getCollection("engine_state_account", Account.class);
        this.orderCollection = database.getCollection("engine_state_order", Order.class);
        this.orderCollection.createIndex(Indexes.ascending("time", "_id"));
        this.productMongoCollection = database.getCollection("engine_state_product", Product.class);
        this.orderBookStateCollection = database.getCollection("engine_state_order_book", OrderBookState.class);
    }

    public List<Product> getProducts() {
        return this.productMongoCollection.find().into(new ArrayList<>());
    }

    public List<Account> getAccounts() {
        return this.accountCollection.find().into(new ArrayList<>());
    }

    public List<Order> getOrders() {
        return this.orderCollection.find()
            .sort(Sorts.ascending("time", "_id"))
            .into(new ArrayList<>());
    }

    public List<OrderBookState> getOrderBookStates() {
        return orderBookStateCollection.find().into(new ArrayList<>());
    }

    public EngineState getEngineState() {
        return engineStateCollection.find(Filters.eq("_id", "default")).first();
    }

    public Long getCommandOffset() {
        EngineState engineState = getEngineState();
        return engineState != null ? engineState.getCommandOffset() : null;
    }

    public void save(Long commandOffset, Collection<Account> accounts, Collection<Order> orders,
        Collection<Product> products, Map<String, Long> tradeIds, Map<String, Long> sequences) {
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            try {
                saveEngineState(commandOffset, session);
                saveProducts(products, session);
                saveAccounts(accounts, session);
                saveOrders(orders, session);
                saveOrderBookState(tradeIds, sequences, session);
                session.commitTransaction();
            } catch (Exception e) {
                session.abortTransaction();
                throw new RuntimeException(e);
            }
        }
    }

    private void saveEngineState(Long commandOffset, ClientSession session) {
        EngineState engineState = new EngineState();
        engineState.setId("default");
        engineState.setCommandOffset(commandOffset);
        WriteModel<EngineState> engineWriteModel = new ReplaceOneModel<>(Filters.eq("_id", engineState.getId()),
            engineState, new ReplaceOptions().upsert(true));
        engineStateCollection.bulkWrite(session, Collections.singletonList(engineWriteModel),
            new BulkWriteOptions().ordered(true));
    }

    private void saveProducts(Collection<Product> products, ClientSession session) {
        if (products.isEmpty()) {
            return;
        }
        List<WriteModel<Product>> writeModels = new ArrayList<>();
        for (Product item : products) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Product> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        productMongoCollection.bulkWrite(session, writeModels, new BulkWriteOptions().ordered(true));
    }

    private void saveOrderBookState(Map<String, Long> tradeIds, Map<String, Long> sequences, ClientSession session) {
        if (tradeIds.isEmpty() && sequences.isEmpty()) {
            return;
        }
        Map<String, OrderBookState> orderBookStates = new HashMap<>();
        tradeIds.forEach((productId, tradeId) -> {
            OrderBookState orderBookState = orderBookStates.get(productId);
            if (orderBookState == null) {
                orderBookState = orderBookStateCollection
                    .find(Filters.eq("id", productId))
                    .first();
                if (orderBookState == null) {
                    orderBookState = new OrderBookState();
                    orderBookState.setId(productId);
                    orderBookStates.put(orderBookState.getId(), orderBookState);
                }
            }
            orderBookState.setTradeId(tradeId);
        });
        sequences.forEach((productId, sequence) -> {
            OrderBookState orderBookState = orderBookStates.get(productId);
            if (orderBookState == null) {
                orderBookState = orderBookStateCollection
                    .find(Filters.eq("id", productId))
                    .first();
                if (orderBookState == null) {
                    orderBookState = new OrderBookState();
                    orderBookState.setId(productId);
                    orderBookStates.put(orderBookState.getId(), orderBookState);
                }
            }
            orderBookState.setSequence(sequence);
        });

        List<WriteModel<OrderBookState>> writeModels = new ArrayList<>();
        orderBookStates.forEach((productId, orderBookState) -> {
            Bson filter = Filters.eq("_id", productId);
            WriteModel<OrderBookState> writeModel = new ReplaceOneModel<>(filter, orderBookState,
                new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        });
        orderBookStateCollection.bulkWrite(session, writeModels, new BulkWriteOptions().ordered(false));
    }

    private void saveOrders(Collection<Order> orders, ClientSession session) {
        if (orders.isEmpty()) {
            return;
        }
        List<WriteModel<Order>> writeModels = new ArrayList<>();
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
        orderCollection.bulkWrite(session, writeModels, new BulkWriteOptions().ordered(false));
    }

    private void saveAccounts(Collection<Account> accounts, ClientSession session) {
        if (accounts.isEmpty()) {
            return;
        }
        List<WriteModel<Account>> writeModels = new ArrayList<>();
        for (Account item : accounts) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Account> writeModel = new ReplaceOneModel<>(filter, item,
                new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        accountCollection.bulkWrite(session, writeModels, new BulkWriteOptions().ordered(false));
    }

}
