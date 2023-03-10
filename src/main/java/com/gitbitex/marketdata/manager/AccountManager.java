package com.gitbitex.marketdata.manager;

import java.util.Collection;
import java.util.List;

import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.repository.AccountRepository;
import com.gitbitex.marketdata.repository.BillRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountManager {
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;

    public AccountManager(AccountRepository accountRepository, BillRepository billRepository) {
        this.accountRepository = accountRepository;
        this.billRepository = billRepository;
    }

    public List<Account> getAccounts(String userId) {
        return accountRepository.findAccountsByUserId(userId);
    }

    public void saveAll(Collection<Account> accounts) {
        accountRepository.saveAll(accounts);
    }
}
