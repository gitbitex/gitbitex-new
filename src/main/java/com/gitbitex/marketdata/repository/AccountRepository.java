package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends JpaRepository<Account, Long>, CrudRepository<Account, Long>,
    JpaSpecificationExecutor<Account> {

    Account findAccountByUserIdAndCurrency(String userId, String currency);
}
