package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.springframework.stereotype.Component;

@Component
public class UserRepository {
    private final MongoCollection<User> collection;

    public UserRepository(MongoDatabase database) {
        this.collection = database.getCollection(User.class.getSimpleName().toLowerCase(), User.class);
        this.collection.createIndex(Indexes.descending("email"), new IndexOptions().unique(true));
    }

    public User findByEmail(String email) {
        return this.collection
                .find(Filters.eq("email", email))
                .first();
    }

    public User findByUserId(String userId) {
        return this.collection
                .find(Filters.eq("_id", userId))
                .first();
    }

    public void save(User user) {
        this.collection.insertOne(user);
    }

}
