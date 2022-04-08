package com.gitbitex.module.account;

import java.math.BigDecimal;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.module.order.entity.Fill;
import com.gitbitex.module.order.entity.Order;
import com.gitbitex.module.product.entity.Product;
import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.kafka.KafkaConsumerThread;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.module.account.command.AccountCommand;
import com.gitbitex.module.account.command.AccountCommandDispatcher;
import com.gitbitex.module.account.command.AccountCommandHandler;
import com.gitbitex.module.account.command.PlaceOrderCommand;
import com.gitbitex.module.account.command.SettleOrderCommand;
import com.gitbitex.module.account.command.SettleOrderFillCommand;
import com.gitbitex.module.matchingengine.command.NewOrderCommand;
import com.gitbitex.module.order.OrderManager;
import com.gitbitex.module.product.ProductManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class AccountantThread extends KafkaConsumerThread<String, AccountCommand> implements AccountCommandHandler {
    private final AccountCommandDispatcher messageDispatcher;
    private final AccountManager accountManager;
    private final ProductManager productManager;
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;

    public AccountantThread(KafkaConsumer<String, AccountCommand> consumer,
        AccountManager accountManager, OrderManager orderManager, ProductManager productManager,
        KafkaMessageProducer messageProducer, AppProperties appProperties) {
        super(consumer, logger);
        this.messageDispatcher = new AccountCommandDispatcher(this);
        this.accountManager = accountManager;
        this.orderManager = orderManager;
        this.productManager = productManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, AccountCommand> consumer) {
        consumer.subscribe(Collections.singletonList(appProperties.getAccountCommandTopic()));
    }

    @Override
    protected void processRecords(KafkaConsumer<String, AccountCommand> consumer,
        ConsumerRecords<String, AccountCommand> records) {
        for (ConsumerRecord<String, AccountCommand> record : records) {
            logger.info("- {} {}", record.offset(), JSON.toJSONString(record.value()));
            this.messageDispatcher.dispatch(record.value());
        }
        consumer.commitSync();
    }

    @Override
    @SneakyThrows
    public void on(PlaceOrderCommand command) {
        Order order = command.getOrder();
        Product product = productManager.getProductById(order.getProductId());

        command.getOrder().setStatus(Order.OrderStatus.NEW);

        String billId = command.getType() + "-" + order.getOrderId();
        if (accountManager.getBillById(billId) == null) {
            String holdCurrency = order.getSide() == Order.OrderSide.BUY ? product.getQuoteCurrency()
                : product.getBaseCurrency();
            BigDecimal holdAmount = order.getSide() == Order.OrderSide.BUY ? order.getFunds() : order.getSize();

            try {
                accountManager.hold(order.getUserId(), holdCurrency, holdAmount, billId);
            } catch (ServiceException e) {
                logger.error("process error: {}", e.getMessage(), e);
                if (e.getCode() != ErrorCode.DUPLICATE_BILL_ID) {
                    command.getOrder().setStatus(Order.OrderStatus.DENIED);
                }
            } catch (Exception e) {
                logger.error("process error: {}", e.getMessage(), e);
                command.getOrder().setStatus(Order.OrderStatus.DENIED);
            }
        }

        NewOrderCommand newOrderCommand = new NewOrderCommand();
        newOrderCommand.setProductId(command.getOrder().getProductId());
        newOrderCommand.setOrder(command.getOrder());
        messageProducer.sendToMatchingEngine(newOrderCommand);
    }

    @Override
    public void on(SettleOrderFillCommand command) {
        Fill fill = orderManager.getFillById(command.getFillId());
        Order order = orderManager.findByOrderId(fill.getOrderId());
        Product product = productManager.getProductById(order.getProductId());
        String userId = order.getUserId();

        String baseBillId = command.getType() + "-base-" + fill.getFillId();
        String quoteBillId = command.getType() + "-quote-" + fill.getFillId();

        if (fill.getSide() == Order.OrderSide.BUY) {
            if (accountManager.getBillById(baseBillId) == null) {
                accountManager.increaseAvailable(userId, product.getBaseCurrency(), fill.getSize(), baseBillId);
            }
            if (accountManager.getBillById(quoteBillId) == null) {
                accountManager.increaseHold(userId, product.getQuoteCurrency(), fill.getFunds().negate(), quoteBillId);
            }
        } else {
            if (accountManager.getBillById(baseBillId) == null) {
                accountManager.increaseHold(userId, product.getBaseCurrency(), fill.getSize().negate(), baseBillId);
            }
            if (accountManager.getBillById(quoteBillId) == null) {
                accountManager.increaseAvailable(userId, product.getQuoteCurrency(), fill.getFunds(), quoteBillId);
            }
        }
    }

    @Override
    public void on(SettleOrderCommand command) {
        Order order = orderManager.findByOrderId(command.getOrderId());
        Product product = productManager.getProductById(order.getProductId());

        String billId = command.getType() + "-" + order.getOrderId();
        if (accountManager.getBillById(billId) != null) {
            return;
        }

        if (order.getSide() == Order.OrderSide.BUY) {
            BigDecimal remainingFunds = order.getFunds().subtract(order.getExecutedValue());
            if (remainingFunds.compareTo(BigDecimal.ZERO) > 0) {
                accountManager.unhold(order.getUserId(), product.getQuoteCurrency(), remainingFunds, billId);
            }
        } else {
            BigDecimal remainingSize = order.getSize().subtract(order.getFilledSize());
            if (remainingSize.compareTo(BigDecimal.ZERO) > 0) {
                accountManager.unhold(order.getUserId(), product.getBaseCurrency(), remainingSize, billId);
            }
        }
    }

}



