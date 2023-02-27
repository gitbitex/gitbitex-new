package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Fill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface FillRepository extends MongoRepository<Fill, Long>, CrudRepository<Fill, Long> {

    boolean existsByOrderIdAndTradeId(String orderId, long traderId);

    Fill findByOrderIdAndTradeId(String orderId, long traderId);

    Fill findByFillId(String fillId);

}
