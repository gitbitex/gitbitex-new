package com.gitbitex.liquidity;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderType;
import com.gitbitex.marketdata.entity.Order;
import com.gitbitex.matchingengine.command.PlaceOrderCommand;
import com.gitbitex.order.ClientOrderReceiver;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class CoinbaseTrader {
    private static final RateLimiter rateLimiter = RateLimiter.create(10);
    private final ClientOrderReceiver clientOrderReceiver;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final AppProperties appProperties;

    //@PostConstruct
    public void init() throws URISyntaxException {

        if (appProperties.getLiquidityTraderUserIds().isEmpty()) {
            return;
        }
        logger.info("start");

        MyClient client = new MyClient(new URI("wss://ws-feed.exchange.coinbase.com"));

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {
                if (!client.isOpen()) {
                    try {
                        if (client.getReadyState().equals(ReadyState.NOT_YET_CONNECTED)) {
                            logger.info("connecting...: {}", client.getURI());
                            client.connectBlocking();
                        } else if (client.getReadyState().equals(ReadyState.CLOSING) || client.getReadyState().equals(
                            ReadyState.CLOSED)) {
                            logger.info("reconnecting...: {}", client.getURI());
                            client.reconnectBlocking();
                        }
                    } catch (Exception e) {
                        logger.error("ws error ", e);
                    }
                } else {
                    client.sendPing();
                }
            } catch (Exception e) {
                logger.error("send ping error: {}", e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    @Getter
    @Setter
    public static class ChannelMessage {
        private String type;
        private String product_id;
        private long tradeId;
        private long sequence;
        private String taker_order_id;
        private String maker_order_id;
        private String time;
        private String size;
        private String price;
        private String side;
        private String orderId;
        private String remaining_size;
        private String funds;
        private String order_type;
        private String reason;
    }

    public class MyClient extends org.java_websocket.client.WebSocketClient {
        //String productId = "BTC-USDT";
        String userId = "32829815-083d-433e-94ba-0fbfe1559552";

        public MyClient(URI serverUri) {
            super(serverUri, new Draft_6455(), null, 1000);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            logger.info("open");

            send("{\"type\":\"subscribe\",\"product_ids\":[\"BTC-USD\"],\"channels\":[\"full\"],\"token\":\"\"}");
        }

        @Override
        public void onMessage(String s) {
            //logger.info(s);
            executor.execute(() -> {
                try {
                    ChannelMessage message = JSON.parseObject(s, ChannelMessage.class);
                    String productId = message.getProduct_id() + "T";
                    switch (message.getType()) {
                        case "received":
                            logger.info(JSON.toJSONString(message));

                            if (message.getPrice() != null) {
                                Order order=new Order();
                                order.setOrderId(UUID.randomUUID().toString());
                                order.setProductId(productId);
                                order.setClientOid(UUID.randomUUID().toString());
                                order.setPrice(new BigDecimal(message.getPrice()));
                                order.setSize(new BigDecimal(message.getSize()));
                                order.setSide(OrderSide.valueOf(message.getSide().toUpperCase()));
                                order.setType(OrderType.LIMIT);
                                order.setUserId(userId);
                                //clientOrderReceiver.handlePlaceOrderRequest(order);


                                /*
                                orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId,
                                        OrderType.LIMIT,
                                        OrderSide.valueOf(message.getSide().toUpperCase()),
                                        new BigDecimal(message.getSize()),
                                        new BigDecimal(message.getPrice()), null, null, null);
                                */
                            } else if (message.getFunds() != null) {
                                /*
                                orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId,
                                        OrderType.MARKET,
                                        OrderSide.valueOf(message.getSide().toUpperCase()), null, null,
                                        new BigDecimal(message.getFunds()), null, null);
                                */
                            }
                            break;
                        case "done":
                            //orderManager.cancelOrder(message.getOrderId(), userId, productId);
                            break;
                        default:
                    }
                } catch (Exception e) {
                    logger.error("error: {}", e.getMessage(), e);
                }
            });
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            logger.info("connection closed");
        }

        @Override
        public void onError(Exception e) {
            logger.error("error", e);
        }
    }
}
