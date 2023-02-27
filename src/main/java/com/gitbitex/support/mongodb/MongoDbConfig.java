package com.gitbitex.support.mongodb;

import com.gitbitex.support.redis.RedisProperties;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.mongodb.client.model.Filters.eq;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class MongoDbConfig {


    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(RedisProperties redisProperties) {
        String uri = "mongodb://root:root@localhost/ex?authSource=admin";
        return  MongoClients.create(uri) ;
    }
}



