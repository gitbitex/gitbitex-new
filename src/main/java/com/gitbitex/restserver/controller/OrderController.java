package com.gitbitex.restserver.controller;

import com.gitbitex.entity.Order;
import com.gitbitex.entity.User;
import com.gitbitex.orderprocessor.OrderManager;
import com.gitbitex.repository.OrderRepository;
import com.gitbitex.restserver.model.OrderDto;
import com.gitbitex.restserver.model.PagedList;
import com.gitbitex.restserver.model.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderManager orderManager;
    private final OrderRepository orderRepository;

    @PostMapping(value = "/orders")
    @SneakyThrows
    public OrderDto placeOrder(@RequestBody @Valid PlaceOrderRequest request,
                               @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Order order = new Order();
        order.setUserId(currentUser.getUserId());
        order.setProductId(request.getProductId());
        order.setType(Order.OrderType.valueOf(request.getType().toUpperCase()));
        order.setSide(Order.OrderSide.valueOf(request.getSide().toUpperCase()));
        if (request.getPrice() != null) {
            order.setPrice(new BigDecimal(request.getPrice()));
        }
        order.setSize(new BigDecimal(request.getSize()));
        order.setTimeInForce(request.getTimeInForce());
        order.setClientOid(request.getClientOid());
        if (request.getFunds() != null) {
            order.setFunds(new BigDecimal(request.getFunds()));
        }
        orderManager.placeOrder(order);
        return orderDto(order);
    }

    @DeleteMapping("/orders/{orderId}")
    @SneakyThrows
    public void cancelOrder(@PathVariable String orderId, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Order order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found: " + orderId);
        }
        if (!order.getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        orderManager.cancelOrder(order);
    }

    @DeleteMapping("/orders")
    @SneakyThrows
    public void cancelOrders(String productId, String side, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Page<Order> orderPage = orderRepository.findAll(currentUser.getUserId(), productId, Order.OrderStatus.OPEN,
                side != null ? Order.OrderSide.valueOf(side.toUpperCase()) : null, 1, 10000);

        for (Order order : orderPage.getContent()) {
            orderManager.cancelOrder(order);
        }
    }

    @GetMapping("/orders")
    public PagedList<OrderDto> listOrders(@RequestParam(required = false) String productId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "50") int pageSize,
                                          @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Order.OrderStatus orderStatus = status != null ? Order.OrderStatus.valueOf(status.toUpperCase()) : null;

        Page<Order> orderPage = orderRepository.findAll(currentUser.getUserId(), productId, orderStatus, null, page,
                pageSize);
        return new PagedList<>(
                orderPage.getContent().stream().map(this::orderDto).collect(Collectors.toList()),
                orderPage.getTotalElements());
    }

    private OrderDto orderDto(Order order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getOrderId());
        orderDto.setPrice(order.getPrice().toPlainString());
        orderDto.setSize(order.getSize().toPlainString());
        orderDto.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().toPlainString() : "0");
        orderDto.setFunds(order.getFunds() != null ? order.getFunds().toPlainString() : "0");
        orderDto.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().toPlainString() : "0");
        orderDto.setSide(order.getSide().name().toLowerCase());
        orderDto.setProductId(order.getProductId());
        orderDto.setType(order.getType().name().toLowerCase());
        if (order.getCreatedAt() != null) {
            orderDto.setCreatedAt(order.getCreatedAt().toInstant().toString());
        }
        if (order.getStatus() != null) {
            orderDto.setStatus(order.getStatus().name().toLowerCase());
        }
        return orderDto;
    }

}
