package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.matchingengine.log.AccountChangeLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AccountBook {
    @Getter
    private final Map<String, Map<String, Account>> accounts = new HashMap<>();
    private final LogWriter logWriter;
    private final AtomicLong sequence;

    public AccountBook(List<Account> accounts, LogWriter logWriter, AtomicLong sequence) {
        this.logWriter = logWriter;
        this.sequence = sequence;
        if (accounts != null) {
            this.addAll(accounts);
        }
    }

    public void addAll(List<Account> accounts) {
        for (Account account : accounts) {
            this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>()).put(account.getCurrency(), account);
        }
    }

    public List<Account> getAllAccounts(){
        return accounts.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public Map<String,Account> getAccountsByUserId(String userId){
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

    private Account createAccount(String userId, String currency) {
        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setAvailable(BigDecimal.ZERO);
        account.setHold(BigDecimal.ZERO);
        this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>()).put(account.getCurrency(), account);
        return account;
    }

    public void deposit(String userId, String currency, BigDecimal amount, String transactionId) {
        Account account = getAccount(userId, currency);
        if (account == null) {
            account = createAccount(userId, currency);
        }
        account.setAvailable(account.getAvailable().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void hold(Account account, BigDecimal amount) {
        if (account == null) {
            throw new NullPointerException("account");
        }
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void unhold(Account account, BigDecimal amount) {
        if (account == null) {
            throw new NullPointerException("account");
        }
        account.setAvailable(account.getAvailable().add(amount));
        account.setHold(account.getHold().subtract(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void incrAvailable(Account account, BigDecimal amount) {
        if (account == null || account.getAvailable().compareTo(amount) < 0) {
            throw new RuntimeException("Account available balance is insufficient");
        }

        account.setAvailable(account.getAvailable().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, BigDecimal.ZERO, amount);
        logWriter.add(accountChangeLog);
    }

    public void incrHold(Account account, BigDecimal amount) {
        if (account == null || account.getAvailable().compareTo(amount) < 0) {
            throw new RuntimeException("Account available balance is insufficient");
        }

        account.setHold(account.getHold().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void exchange(Account takerBaseAccount, Account takerQuoteAccount,
                         Account makerBaseAccount, Account makerQuoteAccount,
                         OrderSide takerSide, BigDecimal size, BigDecimal funds) {
        if (takerSide == OrderSide.BUY) {
            if (takerBaseAccount == null) {
                takerBaseAccount = createAccount(takerQuoteAccount.getUserId(), makerBaseAccount.getCurrency());
            }
            if (makerQuoteAccount == null) {
                makerQuoteAccount = createAccount(makerBaseAccount.getUserId(), takerQuoteAccount.getCurrency());
            }
            takerBaseAccount.setAvailable(takerBaseAccount.getAvailable().add(size));
            takerQuoteAccount.setHold(takerQuoteAccount.getHold().subtract(funds));
            makerBaseAccount.setHold(makerBaseAccount.getHold().subtract(size));
            makerQuoteAccount.setAvailable(makerQuoteAccount.getAvailable().add(funds));
        } else {
            if (takerQuoteAccount == null) {
                takerQuoteAccount = createAccount(takerBaseAccount.getUserId(), makerQuoteAccount.getCurrency());
            }
            if (makerBaseAccount == null) {
                makerBaseAccount = createAccount(makerQuoteAccount.getUserId(), takerBaseAccount.getCurrency());
            }
            takerBaseAccount.setAvailable(takerBaseAccount.getAvailable().add(size));
            takerQuoteAccount.setHold(takerQuoteAccount.getHold().subtract(funds));
            makerBaseAccount.setHold(makerBaseAccount.getHold().subtract(size));
            makerQuoteAccount.setAvailable(makerQuoteAccount.getAvailable().add(funds));
        }
    }

    public AccountChangeLog accountChangeMessage(Account account, BigDecimal holdIncr, BigDecimal availableIncr) {
        AccountChangeLog accountChangeLog = new AccountChangeLog();
        accountChangeLog.setSequence(sequence.incrementAndGet());
        accountChangeLog.setUserId(account.getUserId());
        accountChangeLog.setCurrency(account.getCurrency());
        accountChangeLog.setHold(account.getHold());
        accountChangeLog.setAvailable(account.getAvailable());
        accountChangeLog.setHoldIncrement(holdIncr);
        accountChangeLog.setAvailableIncrement(availableIncr);
        return accountChangeLog;
    }


}
