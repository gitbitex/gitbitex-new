package com.gitbitex.matchingengine;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.order.entity.Order.OrderSide;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class AccountBook {
    private final Map<String, Account> accounts = new HashMap<>();
    private final LogWriter logWriter;
    private final AtomicLong sequence;

    public AccountBook(AccountBookSnapshot snapshot, LogWriter logWriter, AtomicLong sequence) {
        this.logWriter = logWriter;
        this.sequence = sequence;
        if (snapshot!=null) {
            this.addAll(snapshot.getAccounts());
        }
    }

    public AccountBookSnapshot takeSnapshot() {
        List<Account> copiedAccounts= this.accounts.values().stream()
            .map(x->x)
            .collect(Collectors.toList());

        AccountBookSnapshot snapshot=new AccountBookSnapshot();
        snapshot.setAccounts(copiedAccounts);
        return snapshot;
    }

    public void addAll(List<Account> accounts) {

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

        AccountChangeMessage accountChangeMessage = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeMessage);
    }

    public void hold(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold().add(amount));

        AccountChangeMessage accountChangeMessage = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeMessage);
    }

    public void unhold(String userId, String currency, BigDecimal amount) {
        Account account = getAccount(userId, currency);
        account.setAvailable(account.getAvailable().add(amount));
        account.setHold(account.getHold().subtract(amount));

        AccountChangeMessage accountChangeMessage = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeMessage);
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

        AccountChangeMessage accountChangeMessage = accountChangeMessage(account, BigDecimal.ZERO, amount);
        logWriter.add(accountChangeMessage);
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

        AccountChangeMessage accountChangeMessage = accountChangeMessage(account, amount, BigDecimal.ZERO);
        logWriter.add(accountChangeMessage);
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

    public void restoreLog(AccountChangeMessage log) {
        Account account = createAccount(log.getUserId(), log.getCurrency());
        account.setAvailable(log.getAvailable());
        account.setHold(log.getHold());
    }

    public AccountChangeMessage accountChangeMessage(Account account, BigDecimal holdIncr, BigDecimal availableIncr) {
        AccountChangeMessage accountChangeMessage = new AccountChangeMessage();
        accountChangeMessage.setSequence(sequence.incrementAndGet());
        accountChangeMessage.setUserId(account.userId);
        accountChangeMessage.setCurrency(account.getCurrency());
        accountChangeMessage.setHold(account.getHold());
        accountChangeMessage.setAvailable(account.getAvailable());
        accountChangeMessage.setHoldIncr(holdIncr);
        accountChangeMessage.setAvailableIncr(availableIncr);
        return accountChangeMessage;
    }

    @Getter
    @Setter
    public static class Account {
        private String userId;
        private String currency;
        private BigDecimal available = new BigDecimal(0);
        private BigDecimal hold = new BigDecimal(0);
    }
}
