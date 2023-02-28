package com.gitbitex.marketdata.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gitbitex.marketdata.entity.Account;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

@Component
public class AccountRepository {
    private final MongoCollection<Account> mongoCollection;

    public AccountRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Account.class.getSimpleName(), Account.class);
    }

    public List<Account> findAccountsByUserId(String userId) {
        return mongoCollection.find(Filters.eq("userId", userId)).into(new ArrayList<>());
    }

    public void saveAll(Collection<Account> accounts) {
        List<WriteModel<Account>> writeModels = new ArrayList<>();
        for (Account item : accounts) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Account> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        mongoCollection.bulkWrite(writeModels);
    }
}
