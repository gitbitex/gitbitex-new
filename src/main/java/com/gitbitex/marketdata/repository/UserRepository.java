package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.springframework.stereotype.Component;

@Component
public class UserRepository {
    private final MongoCollection<User> mongoCollection;

    public UserRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(User.class.getSimpleName().toLowerCase(), User.class);
    }

    public User findByEmail(String email) {
        return this.mongoCollection.find(Filters.eq("email", email)).first();
    }

    public User findByUserId(String userId) {
        return this.mongoCollection.find(Filters.eq("_id", userId)).first();
    }

    public void save(User user) {
        this.mongoCollection.insertOne(user);
    }

}
