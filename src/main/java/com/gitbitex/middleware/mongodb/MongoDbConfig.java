package com.gitbitex.middleware.mongodb;

import com.gitbitex.middleware.redis.RedisProperties;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
public class MongoDbConfig {

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(RedisProperties redisProperties) {
        String uri = "mongodb://root:root@localhost/ex?authSource=admin";
        return MongoClients.create(uri);
    }

    @Bean
    public MongoDatabase database(MongoClient mongoClient) {
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        return mongoClient.getDatabase("ex").withCodecRegistry(pojoCodecRegistry);
    }
}



