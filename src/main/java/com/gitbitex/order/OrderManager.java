package com.gitbitex.order;

import com.alibaba.fastjson.JSON;
import com.gitbitex.account.AccountManager;
import com.gitbitex.account.command.CancelOrderCommand;
import com.gitbitex.account.command.PlaceOrderCommand;
import com.gitbitex.exception.ErrorCode;
import com.gitbitex.exception.ServiceException;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.entity.Fill;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderStatus;
import com.gitbitex.order.entity.Order.OrderType;
import com.gitbitex.order.entity.Order.TimeInForcePolicy;
import com.gitbitex.order.repository.FillRepository;
import com.gitbitex.order.repository.OrderRepository;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderManager {
    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private final ProductManager productManager;
    private final FillRepository fillRepository;
    private final KafkaMessageProducer messageProducer;
    private final AccountManager accountManager;

    public String placeOrder(String orderId, String userId, String productId, OrderType orderType, OrderSide side,
                             BigDecimal size,
                             BigDecimal price, BigDecimal funds, String clientOrderId, TimeInForcePolicy timeInForcePolicy)
            throws ExecutionException, InterruptedException {
        Product product = productManager.getProductById(productId);

        // calculate size or funds
        switch (orderType) {
            case LIMIT:
                size = size.setScale(product.getBaseScale(), RoundingMode.DOWN);
                price = price.setScale(product.getQuoteScale(), RoundingMode.DOWN);
                funds = side == Order.OrderSide.BUY ? size.multiply(price) : BigDecimal.ZERO;
                break;
            case MARKET:
                price = BigDecimal.ZERO;
                if (side == Order.OrderSide.BUY) {
                    size = BigDecimal.ZERO;
                    funds = funds.setScale(product.getQuoteScale(), RoundingMode.DOWN);
                } else {
                    size = size.setScale(product.getBaseScale(), RoundingMode.DOWN);
                    funds = BigDecimal.ZERO;
                }
                break;
            default:
                throw new RuntimeException("unknown order type: " + orderType);
        }

        // check size or funds
        if (side == Order.OrderSide.SELL) {
            if (size.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad SELL order: size must be positive");
            }

            if (accountManager.getAvailable(userId, product.getBaseCurrency()).compareTo(size) < 0) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE);
            }
        } else {
            if (funds.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad BUY order: funds must be positive");
            }

            if (accountManager.getAvailable(userId, product.getQuoteCurrency()).compareTo(funds) < 0) {
                throw new ServiceException(ErrorCode.INSUFFICIENT_BALANCE);
            }
        }

        // build order
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setType(orderType);
        order.setSide(side);
        order.setSize(size);
        order.setClientOid(clientOrderId);
        order.setSize(size);
        order.setFunds(funds);
        order.setPrice(price);
        order.setStatus(OrderStatus.NEW);
        order.setTime(new Date());

        // send order to accountant
        PlaceOrderCommand placeOrderCommand = new PlaceOrderCommand();
        placeOrderCommand.setUserId(order.getUserId());
        placeOrderCommand.setOrder(order);
        messageProducer.sendAccountCommand(placeOrderCommand, (metadata, e) -> {
            if (e != null) {
                logger.error("send account command error: {}", e.getMessage(), e);
            }
        });

        return order.getOrderId();
    }

    public void cancelOrder(Order order) throws ExecutionException, InterruptedException {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand();
        cancelOrderCommand.setUserId(order.getUserId());
        cancelOrderCommand.setOrderId(order.getOrderId());
        cancelOrderCommand.setProductId(order.getProductId());
        messageProducer.sendAccountCommand(cancelOrderCommand, null);
    }

    public void cancelOrder(String orderId, String userId, String productId)
            throws ExecutionException, InterruptedException {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand();
        cancelOrderCommand.setUserId(userId);
        cancelOrderCommand.setOrderId(orderId);
        cancelOrderCommand.setProductId(productId);
        messageProducer.sendAccountCommand(cancelOrderCommand, null);
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
        order.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().add(funds) : funds);

        if (order.getSide() == OrderSide.BUY) {
            if (order.getExecutedValue().compareTo(order.getFunds()) > 0) {
                throw new RuntimeException("bad order: " + JSON.toJSONString(order));
            }
        } else {
            if (order.getFilledSize().compareTo(order.getSize()) > 0) {
                throw new RuntimeException("bad order: " + JSON.toJSONString(order));
            }
        }
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
            redissonClient.getTopic("order", StringCodec.INSTANCE).publishAsync(JSON.toJSONString(order));
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
