package com.gitbitex.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.product.ProductManager;
import com.gitbitex.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientOrderReceiver {
    private final KafkaMessageProducer messageProducer;
    private final ProductManager productManager;

    public void handlePlaceOrderRequest(Order order) {
        Product product = productManager.getProductById(order.getProductId());

        formatOrder(order, product);
        validateOrder(order);

        // send order to accountant
        PlaceOrderCommand orderPlacedMessage = new PlaceOrderCommand();
        orderPlacedMessage.setOrder(order);
        messageProducer.sendToMatchingEngine("all", orderPlacedMessage, null);
    }

    public void handleCancelOrderRequest(Order order) {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand();
        cancelOrderCommand.setOrderId(order.getOrderId());
        cancelOrderCommand.setProductId(order.getProductId());
        messageProducer.sendToMatchingEngine(order.getUserId(), cancelOrderCommand, null);
    }

    public void deposit(String userId, String currency, BigDecimal amount,String transactionId) {
        DepositCommand depositCommand = new DepositCommand();
        depositCommand.setUserId(userId);
        depositCommand.setCurrency(currency);
        depositCommand.setAmount(amount);
        depositCommand.setTransactionId(transactionId);
        messageProducer.sendToMatchingEngine("all", depositCommand, null);
    }

    /*public void cancelOrder(String orderId, String userId, String productId)
        throws ExecutionException, InterruptedException {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand();
        cancelOrderCommand.setOrderId(orderId);
        cancelOrderCommand.setProductId(productId);
        messageProducer.sendToAccountant(orderId, cancelOrderCommand, null);
    }*/

    private void formatOrder(Order order, Product product) {
        BigDecimal size = order.getSize();
        BigDecimal price = order.getPrice();
        BigDecimal funds = order.getFunds();
        OrderSide side = order.getSide();

        switch (order.getType()) {
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
                throw new RuntimeException("unknown order type: " + order.getType());
        }

        order.setSize(size);
        order.setPrice(price);
        order.setFunds(funds);
    }

    private void validateOrder(Order order) {
        BigDecimal size = order.getSize();
        BigDecimal funds = order.getFunds();
        OrderSide side = order.getSide();

        if (side == Order.OrderSide.SELL) {
            if (size.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad SELL order: size must be positive");
            }
        } else {
            if (funds.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad BUY order: funds must be positive");
            }
        }
    }

}
