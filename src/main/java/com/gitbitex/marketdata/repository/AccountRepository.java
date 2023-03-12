package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Account;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class AccountRepository {
    private final MongoCollection<Account> collection;

    public AccountRepository(MongoDatabase database) {
        this.collection = database.getCollection(Account.class.getSimpleName().toLowerCase(), Account.class);
        this.collection.createIndex(Indexes.descending("userId", "currency"), new IndexOptions().unique(true));
    }

    public List<Account> findAccountsByUserId(String userId) {
        return collection
                .find(Filters.eq("userId", userId))
                .into(new ArrayList<>());
    }

    public void saveAll(Collection<Account> accounts) {
        List<WriteModel<Account>> writeModels = new ArrayList<>();
        for (Account item : accounts) {
            Bson filter = Filters.eq("userId", item.getUserId());
            filter = Filters.and(filter, Filters.eq("currency", item.getCurrency()));
            WriteModel<Account> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }
}
