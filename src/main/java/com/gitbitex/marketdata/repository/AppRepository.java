package com.gitbitex.marketdata.repository;

import java.util.List;

import com.gitbitex.marketdata.entity.App;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface AppRepository extends MongoRepository<App, Long>, CrudRepository<App, Long>{

    List<App> findByUserId(String userId);

    App findByAppId(String appId);

}
