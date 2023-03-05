package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.enums.OrderSide;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class AccountBook {
    private final Map<String, Map<String, Account>> accounts = new HashMap<>();
    private final DirtyObjectHandler dirtyObjectHandler;



    public void addAll(Set<Account> accounts) {
        for (Account account : accounts) {
            this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>())
                .put(account.getCurrency(), account);
        }
    }

    public void add(Account account){
        this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>())
            .put(account.getCurrency(), account);
    }

    public List<Account> getAllAccounts() {
        return accounts.values().stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public Map<String, Account> getAccountsByUserId(String userId) {
        return this.accounts.get(userId);
    }

    @Nullable
    public Account getAccount(String userId, String currency) {
        Map<String, Account> accountMap = accounts.get(userId);
        if (accountMap != null) {
            return accountMap.get(currency);
        }
        return null;
    }

    public void deposit(String userId, String currency, BigDecimal amount, String transactionId,Long commandOffset) {
        Account account = getAccount(userId, currency);
        if (account == null) {
            account = createAccount(userId, currency);
        }
        account.setAvailable(account.getAvailable().add(amount));

        if (dirtyObjectHandler !=null) {
            dirtyObjectHandler.flush(commandOffset, DirtyObjectList.singletonList(account.clone()));
        }
    }

    public void hold(Account account, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NullPointerException("amount should greater than 0");
        }
        if (account == null || account.getAvailable().compareTo(amount) < 0) {
            throw new NullPointerException("insufficient funds");
        }
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold().add(amount));
    }

    public void unhold(Account account, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NullPointerException("amount should greater than 0");
        }
        if (account == null || account.getHold().compareTo(amount) < 0) {
            throw new NullPointerException("insufficient funds");
        }
        account.setAvailable(account.getAvailable().add(amount));
        account.setHold(account.getHold().subtract(amount));
    }

    public void exchange(Account takerBaseAccount, Account takerQuoteAccount, Account makerBaseAccount,
        Account makerQuoteAccount,  OrderSide takerSide, BigDecimal size, BigDecimal funds) {
        if (takerBaseAccount == null) {
            takerBaseAccount = createAccount(takerQuoteAccount.getUserId(), makerBaseAccount.getCurrency());
        }
        if (makerQuoteAccount == null) {
            makerQuoteAccount = createAccount(makerBaseAccount.getUserId(), takerQuoteAccount.getCurrency());
        }
        if (takerQuoteAccount == null) {
            takerQuoteAccount = createAccount(takerBaseAccount.getUserId(), makerQuoteAccount.getCurrency());
        }
        if (makerBaseAccount == null) {
            makerBaseAccount = createAccount(makerQuoteAccount.getUserId(), takerBaseAccount.getCurrency());
        }

        if (takerSide == OrderSide.BUY) {
            takerBaseAccount.setAvailable(takerBaseAccount.getAvailable().add(size));
            takerQuoteAccount.setHold(takerQuoteAccount.getHold().subtract(funds));
            makerBaseAccount.setHold(makerBaseAccount.getHold().subtract(size));
            makerQuoteAccount.setAvailable(makerQuoteAccount.getAvailable().add(funds));
        } else {
            takerBaseAccount.setAvailable(takerBaseAccount.getAvailable().subtract(size));
            takerQuoteAccount.setHold(takerQuoteAccount.getHold().add(funds));
            makerBaseAccount.setHold(makerBaseAccount.getHold().add(size));
            makerQuoteAccount.setAvailable(makerQuoteAccount.getAvailable().subtract(funds));
        }

        validateAccount(takerBaseAccount);
        validateAccount(takerQuoteAccount);
        validateAccount(makerBaseAccount);
        validateAccount(makerQuoteAccount);
    }

    private void validateAccount(Account account) {
        if (account.getAvailable().compareTo(BigDecimal.ZERO) < 0 || account.getHold().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("bad account: " + JSON.toJSONString(account));
        }
    }

    public Account createAccount(String userId, String currency) {
        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setAvailable(BigDecimal.ZERO);
        account.setHold(BigDecimal.ZERO);
        this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>()).put(account.getCurrency(), account);
        return account;
    }

}
