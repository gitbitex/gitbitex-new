package com.gitbitex.matchingengine;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.matchingengine.log.AccountChangeLog;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class AccountBook {
    @Getter
    private final Map<String, Account> accounts = new HashMap<>();
    private final LogWriter logWriter;
    private final AtomicLong sequence;

    public AccountBook(List<Account> accounts, LogWriter logWriter, AtomicLong sequence) {
        this.logWriter = logWriter;
        this.sequence = sequence;
        if (accounts != null  ) {
            this.addAll(accounts);
        }
    }

    public void addAll(List<Account> accounts) {
        for (Account account : accounts) {
            String key = account.getUserId() + "-" + account.getCurrency();
            this.accounts.put(key, account);
        }
    }

    public BigDecimal getAvailable(String userId, String currency) {
        Account account = getAccount(userId, currency);
        return account != null ? account.getAvailable() : BigDecimal.ZERO;
    }

    @Nullable
    public Account getAccount(String userId, String currency) {
        String key = userId + "-" + currency;
        return accounts.get(key);
    }

    private Account createAccount(String userId, String currency) {
        String key = userId + "-" + currency;
        Account account = new Account();
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setAvailable(BigDecimal.ZERO);
        account.setHold(BigDecimal.ZERO);
        accounts.put(key, account);
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

    public void hold(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);
        if (account == null) {
            throw new NullPointerException("account not found: " + userId + " " + currency);
        }
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void unhold(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);
        if (account == null) {
            throw new NullPointerException("account not found: " + userId + " " + currency);
        }
        account.setAvailable(account.getAvailable().add(amount));
        account.setHold(account.getHold().subtract(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void incrAvailable(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (account == null) {
                account = createAccount(userId, currency);
            }
        } else {
            if (account == null || account.getAvailable().compareTo(amount) < 0) {
                throw new RuntimeException("Account available balance is insufficient");
            }
        }

        account.setAvailable(account.getAvailable().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, BigDecimal.ZERO, amount);
        logWriter.add(accountChangeLog);
    }

    public void incrHold(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (account == null) {
                account = createAccount(userId, currency);
            }
        } else {
            if (account == null || account.getAvailable().compareTo(amount) < 0) {
                throw new RuntimeException("Account available balance is insufficient");
            }
        }

        account.setHold(account.getHold().add(amount));

        AccountChangeLog accountChangeLog = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeLog);
    }

    public void exchange(String takerUserId, String makerUserId, String baseCurrency, String quoteCurrency,
                         OrderSide takerSide, BigDecimal size, BigDecimal funds) {
        if (takerSide == OrderSide.BUY) {
            incrHold(takerUserId, quoteCurrency, funds.negate());
            incrAvailable(takerUserId, baseCurrency, size);
            incrHold(makerUserId, baseCurrency, size.negate());
            incrAvailable(makerUserId, quoteCurrency, funds);
        } else {
            incrHold(makerUserId, quoteCurrency, funds.negate());
            incrAvailable(makerUserId, baseCurrency, size);
            incrHold(takerUserId, baseCurrency, size.negate());
            incrAvailable(takerUserId, quoteCurrency, funds);
        }
    }

    public void restoreLog(AccountChangeLog log) {
        Account account = createAccount(log.getUserId(), log.getCurrency());
        account.setAvailable(log.getAvailable());
        account.setHold(log.getHold());
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
