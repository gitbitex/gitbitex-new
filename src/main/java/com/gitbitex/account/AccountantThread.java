package com.gitbitex.account;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.account.command.AccountCommand;
import com.gitbitex.account.command.AccountCommandDispatcher;
import com.gitbitex.account.command.AccountCommandHandler;
import com.gitbitex.account.command.CancelOrderCommand;
import com.gitbitex.account.command.PlaceOrderCommand;
import com.gitbitex.account.command.SettleOrderCommand;
import com.gitbitex.account.command.SettleOrderFillCommand;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.NewOrderCommand;
import com.gitbitex.order.OrderManager;
import com.gitbitex.order.entity.Fill;
import com.gitbitex.order.entity.Order;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class AccountantThread extends KafkaConsumerThread<String, AccountCommand> implements AccountCommandHandler {
    private final AccountCommandDispatcher messageDispatcher;
    private final AccountManager accountManager;
    private final ProductManager productManager;
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    String userId = "ad80fcfe-c3a1-46a1-acf8-1d6a909b2c5a";

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

    private boolean isTrader(String userId) {
        return userId.equals(this.userId);
    }

    @Override
    protected void doSubscribe(KafkaConsumer<String, AccountCommand> consumer) {
        consumer.subscribe(Collections.singletonList(appProperties.getAccountCommandTopic()),
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    for (TopicPartition partition : partitions) {
                        logger.info("partition assigned: {}", partition.toString());
                    }
                }
            });
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

        if (!isTrader(order.getUserId())) {
            String billId = command.getType() + "-" + order.getOrderId();
            if (accountManager.getBillById(billId) == null) {
                String holdCurrency = order.getSide() == Order.OrderSide.BUY ? product.getQuoteCurrency()
                    : product.getBaseCurrency();
                BigDecimal holdAmount = order.getSide() == Order.OrderSide.BUY ? order.getFunds() : order.getSize();
                try {
                    accountManager.hold(order.getUserId(), holdCurrency, holdAmount, billId);
                } catch (Exception e) {
                    logger.error("hold {} {} for order {} failed: {}", holdCurrency, holdAmount, order.getOrderId(),
                        e.getMessage(), e);
                    command.getOrder().setStatus(Order.OrderStatus.DENIED);
                }
            }
        }

        NewOrderCommand newOrderCommand = new NewOrderCommand();
        newOrderCommand.setProductId(command.getOrder().getProductId());
        newOrderCommand.setOrder(command.getOrder());
        messageProducer.sendToMatchingEngine(newOrderCommand);
    }

    @Override
    public void on(CancelOrderCommand command) {
        com.gitbitex.matchingengine.command.CancelOrderCommand cancelOrderCommand
            = new com.gitbitex.matchingengine.command.CancelOrderCommand();
        cancelOrderCommand.setUserId(command.getUserId());
        cancelOrderCommand.setOrderId(command.getOrderId());
        cancelOrderCommand.setProductId(command.getProductId());
        messageProducer.sendToMatchingEngine(cancelOrderCommand);
    }

    @Override
    public void on(SettleOrderFillCommand command) {
        if (isTrader(command.getUserId())){
            return;
        }

        Fill fill = orderManager.getFillById(command.getFillId());
        Product product = productManager.getProductById(command.getProductId());
        String userId = command.getUserId();

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
        if (isTrader(command.getUserId())){
            return;
        }

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



