package com.gitbitex.openapi.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.gitbitex.openapi.model.OrderDto;
import com.gitbitex.openapi.model.PagedList;
import com.gitbitex.openapi.model.PlaceOrderRequest;
import com.gitbitex.order.ClientOrderReceiver;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.marketdata.entity.Order.OrderSide;
import com.gitbitex.marketdata.entity.Order.OrderStatus;
import com.gitbitex.marketdata.entity.Order.OrderType;
import com.gitbitex.marketdata.entity.Order.TimeInForcePolicy;
import com.gitbitex.marketdata.repository.OrderRepository;
import com.gitbitex.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final ClientOrderReceiver clientOrderReceiver;

    @PostMapping(value = "/orders")
    @SneakyThrows
    public OrderDto placeOrder(@RequestBody @Valid PlaceOrderRequest request,
        @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        OrderType type = Order.OrderType.valueOf(request.getType().toUpperCase());
        OrderSide side = Order.OrderSide.valueOf(request.getSide().toUpperCase());
        BigDecimal size = new BigDecimal(request.getSize());
        BigDecimal price = request.getPrice() != null ? new BigDecimal(request.getPrice()) : null;
        BigDecimal funds = request.getFunds() != null ? new BigDecimal(request.getFunds()) : null;
        TimeInForcePolicy timeInForcePolicy = request.getTimeInForce() != null
            ? TimeInForcePolicy.valueOf(request.getTimeInForce().toUpperCase())
            : null;

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setUserId(currentUser.getUserId());
        order.setProductId(request.getProductId());
        order.setType(type);
        order.setSide(side);
        order.setSize(size);
        order.setClientOid(request.getClientOid());
        order.setSize(size);
        order.setFunds(funds);
        order.setPrice(price);
        order.setStatus(OrderStatus.NEW);
        order.setTime(new Date());

        clientOrderReceiver.handlePlaceOrderRequest(order);

        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getOrderId());
        return orderDto;
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

        clientOrderReceiver.handleCancelOrderRequest(order);
    }

    @DeleteMapping("/orders")
    @SneakyThrows
    public void cancelOrders(String productId, String side, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        OrderSide orderSide = side != null ? Order.OrderSide.valueOf(side.toUpperCase()) : null;

        Page<Order> orderPage = orderRepository.findAll(currentUser.getUserId(), productId, Order.OrderStatus.OPEN,
            orderSide, 1, 20000);

        for (Order order : orderPage.getContent()) {
            clientOrderReceiver.handleCancelOrderRequest(order);
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
