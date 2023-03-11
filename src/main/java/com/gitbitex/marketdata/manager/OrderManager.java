package com.gitbitex.marketdata.manager;

import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.repository.FillRepository;
import com.gitbitex.marketdata.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Slf4j
public class OrderManager {
    private final OrderRepository orderRepository;
    private final FillRepository fillRepository;

    private OrderManager(OrderRepository orderRepository, FillRepository fillRepository) {
        this.orderRepository = orderRepository;
        this.fillRepository = fillRepository;
    }

    public void saveAll(Collection<Order> orders) {
        orderRepository.saveAll(orders);
    }
}
