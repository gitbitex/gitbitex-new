package com.gitbitex.orderprocessor;

import com.alibaba.fastjson.JSON;
import com.gitbitex.accountant.AccountManager;
import com.gitbitex.accountant.command.PlaceOrderCommand;
import com.gitbitex.entity.Fill;
import com.gitbitex.entity.Order;
import com.gitbitex.entity.Product;
import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.feedserver.message.OrderMessage;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.repository.FillRepository;
import com.gitbitex.repository.OrderRepository;
import com.gitbitex.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderManager {
    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final FillRepository fillRepository;
    private final KafkaMessageProducer messageProducer;
    private final AccountManager accountManager;

    public void placeOrder(Order order) throws ExecutionException, InterruptedException {
        Product product = productRepository.findByProductId(order.getProductId());

        // format order
        BigDecimal size = order.getSize();
        BigDecimal price = order.getPrice();
        BigDecimal funds = order.getFunds();
        switch (order.getType()) {
            case LIMIT:
                size = size.setScale(product.getBaseScale(), RoundingMode.DOWN);
                price = price.setScale(product.getQuoteScale(), RoundingMode.DOWN);
                funds = order.getSide() == Order.OrderSide.BUY ? size.multiply(price) : BigDecimal.ZERO;
                break;
            case MARKET:
                price = BigDecimal.ZERO;
                if (order.getSide() == Order.OrderSide.BUY) {
                    size = BigDecimal.ZERO;
                    funds = funds.setScale(product.getQuoteScale(), RoundingMode.DOWN);
                } else {
                    size = size.setScale(product.getBaseScale(), RoundingMode.DOWN);
                    funds = BigDecimal.ZERO;
                }
                break;
            default:
                throw new RuntimeException("unknown order type: " + order.getType());
        }
        order.setOrderId(UUID.randomUUID().toString());
        order.setSize(size);
        order.setFunds(funds);
        order.setPrice(price);

        // check order
        if (order.getSide() == Order.OrderSide.SELL) {
            if (order.getSize().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad SELL order: size must be positive");
            }
        } else {
            if (order.getFunds().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad BUY order: funds must be positive");
            }
        }

        // check account
        if (order.getSide() == Order.OrderSide.BUY) {
            if (accountManager.getAvailable(order.getUserId(), product.getQuoteCurrency()).compareTo(order.getFunds())
                    < 0) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE);
            }
        } else {
            if (accountManager.getAvailable(order.getUserId(), product.getBaseCurrency()).compareTo(order.getFunds())
                    < 0) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE);
            }
        }

        // send order to matching-engine
        PlaceOrderCommand placeOrderCommand = new PlaceOrderCommand();
        placeOrderCommand.setUserId(order.getUserId());
        placeOrderCommand.setOrder(order);
        messageProducer.sendToAccountant(placeOrderCommand);
    }

    public void cancelOrder(Order order) throws ExecutionException, InterruptedException {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand();
        cancelOrderCommand.setUserId(order.getUserId());
        cancelOrderCommand.setOrderId(order.getOrderId());
        cancelOrderCommand.setProductId(order.getProductId());
        messageProducer.sendToMatchingEngine(cancelOrderCommand);
    }

    @Transactional(rollbackFor = Exception.class)
    public String fillOrder(String orderId, long tradeId, BigDecimal size, BigDecimal price, BigDecimal funds) {
        // check if fill has been executed
        Fill fill = fillRepository.findByOrderIdAndTradeId(orderId, tradeId);
        if (fill != null) {
            return fill.getFillId();
        }

        Order order = orderRepository.findByOrderId(orderId);
        order.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().add(size) : size);
        order.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().add(funds) : size);
        save(order);

        fill = new Fill();
        fill.setFillId(UUID.randomUUID().toString());
        fill.setOrderId(orderId);
        fill.setTradeId(tradeId);
        fill.setSize(size);
        fill.setPrice(price);
        fill.setFunds(funds);
        fill.setSide(order.getSide());
        fillRepository.save(fill);

        return fill.getFillId();
    }

    public void save(Order order) {
        orderRepository.save(order);

        // send order update notify
        try {
            OrderMessage orderMessage = new OrderMessage();
            orderMessage.setType("order");
            orderMessage.setUserId(order.getUserId());
            orderMessage.setProductId(order.getProductId());
            orderMessage.setId(order.getOrderId());
            orderMessage.setPrice(order.getPrice().toPlainString());
            orderMessage.setSize(order.getSize().toPlainString());
            orderMessage.setFunds(order.getFunds().toPlainString());
            orderMessage.setSide(order.getSide().name().toLowerCase());
            orderMessage.setOrderType(order.getType().name().toLowerCase());
            orderMessage.setCreatedAt(order.getCreatedAt().toInstant().toString());
            orderMessage.setFillFees(order.getFillFees() != null ? order.getFillFees().toPlainString() : "0");
            orderMessage.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().toPlainString() : "0");
            orderMessage.setExecutedValue(
                    order.getExecutedValue() != null ? order.getExecutedValue().toPlainString() : "0");
            orderMessage.setStatus(order.getStatus().name().toLowerCase());
            redissonClient.getTopic("order", StringCodec.INSTANCE).publish(JSON.toJSONString(orderMessage));
        } catch (Exception e) {
            logger.error("notify error: {}", e.getMessage(), e);
        }
    }

    public Order findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    public Fill getFillById(String id) {
        return fillRepository.findByFillId(id);
    }

}
