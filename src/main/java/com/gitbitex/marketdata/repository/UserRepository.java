package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.User;
 import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends MongoRepository<User, Long>, CrudRepository<User, Long>
    {

    User findByEmail(String email);

    User findByUserId(String userId);

}
