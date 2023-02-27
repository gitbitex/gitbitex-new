package com.gitbitex.marketdata.repository;

import java.util.List;

import com.gitbitex.marketdata.entity.Trade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface TradeRepository extends MongoRepository<Trade, Long>, CrudRepository<Trade, Long> {

    List<Trade> findFirst50ByProductIdOrderByTimeDesc(String productId);

    Trade findFirstByProductIdOrderByTimeDesc(String productId);

    Trade findByProductIdAndTradeId(String productId, long tradeId);

}
