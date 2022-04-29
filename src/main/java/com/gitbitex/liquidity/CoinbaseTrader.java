package com.gitbitex.liquidity;

import com.alibaba.fastjson.JSON;
import com.gitbitex.AppProperties;
import com.gitbitex.order.OrderManager;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class CoinbaseTrader {
    private static final RateLimiter rateLimiter = RateLimiter.create(10);
    private final OrderManager orderManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final AppProperties appProperties;

    @PostConstruct
    public void init() throws URISyntaxException {
        if (appProperties.getLiquidityTraderUserIds().isEmpty()) {
            return;
        }
        logger.info("start");

        MyClient client = new MyClient(new URI("wss://ws-feed.exchange.coinbase.com"));

       /* Executors.newFixedThreadPool(1).execute(() -> {
            try {
                client.connectBlocking();
                logger.info("exit");
            } catch (Exception e) {
                logger.error("error", e);
            }
        });*/

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try {

                if (!client.isOpen() && !client.isConnecting()) {
                    logger.info("reconnecting...");
                    client.connectBlocking();
                }else{
                    client.sendPing();
                }
            } catch (Exception e) {
                logger.error("send ping error: {}", e.getMessage(), e);
            }
        }, 0, 3, TimeUnit.SECONDS);
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
        String userId = "ad80fcfe-c3a1-46a1-acf8-1d6a909b2c5a";

        public MyClient(URI serverUri) {
            super(serverUri, new Draft_6455(), null, 1000);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            logger.info("open");

            send("{\"type\":\"subscribe\",\"product_ids\":[\"BTC-USD\",\"ETH-USD\",\"LTC-USD\",\"ETC-USD\"," +
                    "\"DOGE-USD\",\"ADA-USD\",\"DOT-USD\"],\"channels\":[\"full\"],\"token\":\"\"}");
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
                            if (!rateLimiter.tryAcquire()) {
                                return;
                            }

                            if (message.getPrice() != null) {
                                orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId,
                                        OrderType.LIMIT,
                                        OrderSide.valueOf(message.getSide().toUpperCase()),
                                        new BigDecimal(message.getSize()),
                                        new BigDecimal(message.getPrice()), null, null, null);
                            } else if (message.getFunds() != null) {
                                orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId,
                                        OrderType.MARKET,
                                        OrderSide.valueOf(message.getSide().toUpperCase()), null, null,
                                        new BigDecimal(message.getFunds()), null, null);
                            }
                            break;
                        case "done":
                            orderManager.cancelOrder(message.getOrderId(), userId, productId);
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
