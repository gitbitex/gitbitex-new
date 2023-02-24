package com.gitbitex.order;

import java.math.BigDecimal;
import java.util.UUID;

import com.alibaba.fastjson.JSON;

import com.gitbitex.order.entity.Fill;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderStatus;
import com.gitbitex.order.repository.FillRepository;
import com.gitbitex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderManager {
    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;

    @Transactional(rollbackFor = Exception.class)
    public void fillOrder(String orderId, long tradeId, BigDecimal size, BigDecimal price, BigDecimal funds) {
        // check if fill has been executed
        Fill fill = fillRepository.findByOrderIdAndTradeId(orderId, tradeId);
        if (fill != null) {
            return;
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
        fill.setProductId(order.getProductId());
        fill.setUserId(order.getUserId());
        fillRepository.save(fill);
    }


    public void receiveOrder(Order order) {
        logger.info("[{}] Receive order", order.getOrderId());

        if (findByOrderId(order.getOrderId()) != null) {
            logger.warn("Order already exists ：{}", order.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.RECEIVED);
        save(order);
    }

    public void rejectOrder(Order order) {
        logger.info("[{}] Reject order", order.getOrderId());

        if (findByOrderId(order.getOrderId()) != null) {
            logger.warn("Order already exists ：{}", order.getOrderId());
            return;
        }

        order.setStatus(OrderStatus.REJECTED);
        save(order);
    }

    public void openOrder(String orderId) {
        logger.info("[{}] Open order", orderId);

        Order order = findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.RECEIVED) {
            logger.warn("[{}] Cannot open an order in a non-RECEIVED status,current status: {}", order.getOrderId(),
                order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.OPEN);
        save(order);
    }

    public void closeOrder(String orderId, OrderStatus orderStatus) {
        logger.info("[{}] Close order: reason={}", orderId, orderStatus);

        Order order = findByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("order not found: " + orderId);
        }
        if (order.getStatus() != OrderStatus.RECEIVED && order.getStatus() != OrderStatus.OPEN) {
            logger.warn("[{}] Cannot close an order in a non-RECEIVED or non-OPEN status,current status: {}",
                order.getOrderId(), order.getStatus());
            return;
        }

        order.setStatus(orderStatus);
        save(order);
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

}
