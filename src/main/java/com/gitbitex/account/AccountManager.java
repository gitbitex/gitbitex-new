package com.gitbitex.account;

import java.math.BigDecimal;

import com.alibaba.fastjson.JSON;

import com.gitbitex.account.entity.Account;
import com.gitbitex.account.entity.Bill;
import com.gitbitex.account.repository.AccountRepository;
import com.gitbitex.account.repository.BillRepository;
import com.gitbitex.matchingengine.log.AccountChangeMessage;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.repository.FillRepository;
import com.gitbitex.marketdata.repository.OrderRepository;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
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
    public void reviewOrder(Order order) {
        logger.info("[{}] Review order", order.getOrderId());

        Product product = productManager.getProductById(order.getProductId());

        // prevent orders from being processed repeatedly
        String billId = "NEW_ORDER-" + order.getOrderId();
        if (billRepository.findByBillId(billId) != null) {
            return;
        }

        String currency;
        BigDecimal amount;
        if (order.getSide() == Order.OrderSide.BUY) {
            currency = product.getQuoteCurrency();
            amount = order.getFunds();
        } else {
            currency = product.getBaseCurrency();
            amount = order.getSize();
        }

        hold(order.getUserId(), currency, amount, billId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void fillOrder(OrderFilledMessage message) {
        logger.info("[{}] Fill order", message.getOrderId());

        Product product = productManager.getProductById(message.getProductId());
        String userId = message.getUserId();
        String baseBillId = "FILL_ORDER_BASE-" + message.getTradeId();
        String quoteBillId = "FILL_ORDER_QUOTE-" + message.getTradeId();

        if (message.getSide() == Order.OrderSide.BUY) {
            if (billRepository.findByBillId(baseBillId) == null) {
                increaseAvailable(userId, product.getBaseCurrency(), message.getSize(), baseBillId);
            }
            if (billRepository.findByBillId(quoteBillId) == null) {
                increaseHold(userId, product.getQuoteCurrency(), message.getFunds().negate(), quoteBillId);
            }
        } else {
            if (billRepository.findByBillId(baseBillId) == null) {
                increaseHold(userId, product.getBaseCurrency(), message.getSize().negate(), baseBillId);
            }
            if (billRepository.findByBillId(quoteBillId) == null) {
                increaseAvailable(userId, product.getQuoteCurrency(), message.getFunds(), quoteBillId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(OrderDoneMessage message) {
        logger.info("[{}] Close order", message.getOrderId());

        Product product = productManager.getProductById(message.getProductId());

        String billId = "CLOSE-ORDER-" + message.getOrderId();
        if (billRepository.findByBillId(billId) != null) {
            return;
        }

        if (message.getSide() == Order.OrderSide.BUY) {
            //BigDecimal remainingFunds = order.getFunds().subtract(order.getExecutedValue());
            if (message.getRemainingFunds().compareTo(BigDecimal.ZERO) > 0) {
                unhold(message.getUserId(), product.getQuoteCurrency(), message.getRemainingFunds(), billId);
            }
        } else {
            //BigDecimal remainingSize = order.getSize().subtract(order.getFilledSize());
            if (message.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                unhold(message.getUserId(), product.getBaseCurrency(), message.getRemainingSize(), billId);
            }
        }
    }

    private void hold(String userId, String currency, BigDecimal amount, String billId) {
        logger.info("hold {} {} {}", userId, amount, currency);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be positive");
        }

        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null || account.getAvailable() == null || account.getAvailable().compareTo(amount) < 0) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE,
                String.format("insufficient balance：%s", currency));
        }
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold() != null ? account.getHold().add(amount) : amount);
        save(account);

        Bill bill = new Bill();
        bill.setBillId(billId);
        bill.setUserId(userId);
        bill.setCurrency(currency);
        bill.setBillId(billId);
        bill.setHoldIncrement(amount);
        bill.setAvailableIncrement(amount.negate());
        billRepository.save(bill);
    }

    private void unhold(String userId, String currency, BigDecimal amount, String billId) {
        logger.info("unhold {} {} {}", userId, amount, currency);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be positive");
        }

        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null || account.getHold() == null || account.getHold().compareTo(amount) < 0) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE,
                String.format("insufficient hold balance：%s", currency));
        }
        account.setAvailable(account.getAvailable() != null ? account.getAvailable().add(amount) : amount);
        account.setHold(account.getHold().subtract(amount));
        save(account);

        Bill bill = new Bill();
        bill.setBillId(billId);
        bill.setUserId(userId);
        bill.setCurrency(currency);
        bill.setBillId(billId);
        bill.setHoldIncrement(amount.negate());
        bill.setAvailableIncrement(amount);
        billRepository.save(bill);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deposit(AccountChangeMessage message) {
        String userId = message.getUserId();
        String currency = message.getCurrency();
        String billId = "DEPOSIT-" + message.getTransactionId();

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

    @Transactional(rollbackFor = Exception.class)
    public void increaseAvailable(String userId, String currency, BigDecimal amount, String billId) {
        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null) {
            account = new Account();
            account.setUserId(userId);
            account.setCurrency(currency);
        }
        account.setAvailable(account.getAvailable() != null ? account.getAvailable().add(amount) : amount);
        save(account);

        Bill bill = new Bill();
        bill.setBillId(billId);
        bill.setUserId(userId);
        bill.setCurrency(currency);
        bill.setAvailableIncrement(amount);
        billRepository.save(bill);
    }

    private void increaseHold(String userId, String currency, BigDecimal amount, String billId) {
        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null) {
            account = new Account();
            account.setUserId(userId);
            account.setCurrency(currency);
        }
        account.setHold(account.getHold() != null ? account.getHold().add(amount) : amount);
        save(account);

        Bill bill = new Bill();
        bill.setBillId(billId);
        bill.setUserId(userId);
        bill.setCurrency(currency);
        bill.setHoldIncrement(amount);
        billRepository.save(bill);
    }

    private void save(Account account) {
        validateAccount(account);
        accountRepository.save(account);
        tryNotifyAccountUpdate(account);
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
