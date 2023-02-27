package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Bill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface BillRepository extends MongoRepository<Bill, Long>, CrudRepository<Bill, Long> {
    boolean existsByBillId(String billId);

    Bill findByBillId(String billId);

}
