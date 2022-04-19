package com.gitbitex.account;

import com.alibaba.fastjson.JSON;
import com.gitbitex.account.entity.Account;
import com.gitbitex.account.entity.Bill;
import com.gitbitex.account.repository.AccountRepository;
import com.gitbitex.account.repository.BillRepository;
import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.order.entity.Fill;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.repository.FillRepository;
import com.gitbitex.order.repository.OrderRepository;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Slf4j
@Component
public class AccountManager {
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final RedissonClient redissonClient;
    private final ProductManager productManager;
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;

    public BigDecimal getAvailable(String userId, String currency) {
        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        return account != null ? account.getAvailable() : BigDecimal.ZERO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void holdFundsForNewOrder(Order order) {
        Product product = productManager.getProductById(order.getProductId());

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
    public void settleOrderFill(String fillId, String userId) {
        Fill fill = fillRepository.findByFillId(fillId);
        Product product = productManager.getProductById(fill.getProductId());
        String baseBillId = "SETTLE_ORDER_FILL" + "-BASE-" + fillId;
        String quoteBillId = "SETTLE_ORDER_FILL" + "-QUOTE-" + fillId;

        if (fill.getSide() == Order.OrderSide.BUY) {
            if (billRepository.findByBillId(baseBillId) == null) {
                increaseAvailable(userId, product.getBaseCurrency(), fill.getSize(), baseBillId);
            }
            if (billRepository.findByBillId(quoteBillId) == null) {
                increaseHold(userId, product.getQuoteCurrency(), fill.getFunds().negate(), quoteBillId);
            }
        } else {
            if (billRepository.findByBillId(baseBillId) == null) {
                increaseHold(userId, product.getBaseCurrency(), fill.getSize().negate(), baseBillId);
            }
            if (billRepository.findByBillId(quoteBillId) == null) {
                increaseAvailable(userId, product.getQuoteCurrency(), fill.getFunds(), quoteBillId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void settleOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId);
        Product product = productManager.getProductById(order.getProductId());

        String billId = "SETTLE_ORDER-" + order.getOrderId();
        if (billRepository.findByBillId(billId) != null) {
            return;
        }

        if (order.getSide() == Order.OrderSide.BUY) {
            BigDecimal remainingFunds = order.getFunds().subtract(order.getExecutedValue());
            if (remainingFunds.compareTo(BigDecimal.ZERO) > 0) {
                unhold(order.getUserId(), product.getQuoteCurrency(), remainingFunds, billId);
            }
        } else {
            BigDecimal remainingSize = order.getSize().subtract(order.getFilledSize());
            if (remainingSize.compareTo(BigDecimal.ZERO) > 0) {
                unhold(order.getUserId(), product.getBaseCurrency(), remainingSize, billId);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void hold(String userId, String currency, BigDecimal amount, String billId) {
        logger.info("hold {} {} {}", userId, currency, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be positive");
        }

        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null || account.getAvailable().compareTo(amount) < 0) {
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

    @Transactional(rollbackFor = Exception.class)
    public void unhold(String userId, String currency, BigDecimal amount, String billId) {
        logger.info("unhold {} {} {}", userId, currency, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amount must be positive");
        }

        checkBillId(billId);

        Account account = accountRepository.findAccountByUserIdAndCurrency(userId, currency);
        if (account == null || account.getHold().compareTo(amount) < 0) {
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

    public Bill getBillById(String billId) {
        return billRepository.findByBillId(billId);
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

    @Transactional(rollbackFor = Exception.class)
    public void increaseHold(String userId, String currency, BigDecimal amount, String billId) {
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
        checkAccount(account);
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

    private void checkAccount(Account account) {
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
