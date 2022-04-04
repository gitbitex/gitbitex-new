package com.gitbitex.repository;

import com.gitbitex.entity.Fill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface FillRepository extends JpaRepository<Fill, Long>, CrudRepository<Fill, Long>,
        JpaSpecificationExecutor<Fill> {

    boolean existsByOrderIdAndTradeId(String orderId, long traderId);

    Fill findByOrderIdAndTradeId(String orderId, long traderId);

    Fill findByFillId(String fillId);

}
