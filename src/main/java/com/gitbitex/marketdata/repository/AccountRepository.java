package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends MongoRepository<Account, Long>, CrudRepository<Account, Long> {

    Account findAccountByUserIdAndCurrency(String userId, String currency);
}
