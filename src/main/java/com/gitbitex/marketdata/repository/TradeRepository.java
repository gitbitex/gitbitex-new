package com.gitbitex.marketdata.repository;

import java.util.List;

import com.gitbitex.marketdata.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface TradeRepository extends JpaRepository<Trade, Long>, CrudRepository<Trade, Long>,
    JpaSpecificationExecutor<Trade> {

    List<Trade> findFirst50ByProductIdOrderByTimeDesc(String productId);

    Trade findFirstByProductIdOrderByTimeDesc(String productId);

    Trade findByProductIdAndTradeId(String productId, long tradeId);

}
