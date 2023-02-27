package com.gitbitex.marketdata.manager;

import java.math.BigDecimal;

import com.alibaba.fastjson.JSON;

import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.marketdata.entity.Account;
import com.gitbitex.marketdata.repository.AccountRepository;
import com.gitbitex.marketdata.repository.BillRepository;
import com.gitbitex.marketdata.repository.FillRepository;
import com.gitbitex.marketdata.repository.OrderRepository;
import com.gitbitex.matchingengine.log.AccountMessage;
import com.gitbitex.product.ProductManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Component
public class AccountManager {
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final RedissonClient redissonClient;
    private final ProductManager productManager;
    private final FillRepository fillRepository;
    private final OrderRepository orderRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deposit(AccountMessage message) {
        String userId = message.getUserId();
        String currency = message.getCurrency();
        String billId ;//= "DEPOSIT-" + message.getTransactionId();

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null) {
            account = new Account();
            account.setUserId(userId);
            account.setCurrency(currency);
        }
        account.setAvailable(message.getAvailable());
        account.setHold(message.getHold());
        save(account);
    }

    public void save(Account account) {
        //validateAccount(account);
        accountRepository.save(account);
        //tryNotifyAccountUpdate(account);
    }

    private void checkBillId(String billId) {
        if (billId == null) {
            throw new NullPointerException("billId");
        }
        if (billRepository.existsByBillId(billId)) {
            throw new ServiceException(ErrorCode.DUPLICATE_BILL_ID, billId);
        }
    }

    private void validateAccount(Account account) {
        if ((account.getAvailable() != null && account.getAvailable().compareTo(BigDecimal.ZERO) < 0) ||
            (account.getHold() != null && account.getHold().compareTo(BigDecimal.ZERO) < 0)) {
            throw new RuntimeException("bad account: " + JSON.toJSONString(account));
        }
    }

    private void tryNotifyAccountUpdate(Account account) {
        try {
            redissonClient.getTopic("account", StringCodec.INSTANCE).publish(JSON.toJSONString(account));
        } catch (Exception e) {
            logger.error("notify error: {}", e.getMessage(), e);
        }
    }
}
