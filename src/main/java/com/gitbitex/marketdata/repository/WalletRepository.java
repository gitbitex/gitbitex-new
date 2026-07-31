package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Wallet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class WalletRepository {
    private final MongoCollection<Wallet> collection;

    public WalletRepository(MongoDatabase database) {
        this.collection = database.getCollection(Wallet.class.getSimpleName().toLowerCase(), Wallet.class);
        this.collection.createIndex(Indexes.descending("userId", "currency"), new IndexOptions().unique(true));
        this.collection.createIndex(Indexes.ascending("address"), new IndexOptions().unique(true).sparse(true));
    }

    public List<Wallet> findWalletsByUserId(String userId) {
        return collection
                .find(Filters.eq("userId", userId))
                .into(new ArrayList<>());
    }

    public Wallet findWalletById(String id) {
        return collection.find(Filters.eq("_id", id)).first();
    }

    public Wallet findWalletByUserIdAndCurrency(String userId, String currency) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("currency", currency)
        );
        return collection.find(filter).first();
    }

    public Wallet findWalletByAddress(String address) {
        return collection.find(Filters.eq("address", address)).first();
    }

    public void save(Wallet wallet) {
        collection.replaceOne(
                Filters.eq("_id", wallet.getId()),
                wallet,
                new ReplaceOptions().upsert(true)
        );
    }

    public void saveAll(Collection<Wallet> wallets) {
        List<WriteModel<Wallet>> writeModels = new ArrayList<>();
        for (Wallet item : wallets) {
            Bson filter = Filters.eq("userId", item.getUserId());
            filter = Filters.and(filter, Filters.eq("currency", item.getCurrency()));
            WriteModel<Wallet> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }

    public void updateBalance(String walletId, BigDecimal amount, boolean isDebit) {
        Bson filter = Filters.eq("_id", walletId);
        Bson update;
        if (isDebit) {
            update = Updates.inc("balance", amount.negate());
        } else {
            update = Updates.inc("balance", amount);
        }
        collection.updateOne(filter, update);
    }
}
