package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface BillRepository extends JpaRepository<Bill, Long>, CrudRepository<Bill, Long>,
    JpaSpecificationExecutor<Bill> {
    boolean existsByBillId(String billId);

    Bill findByBillId(String billId);

}
