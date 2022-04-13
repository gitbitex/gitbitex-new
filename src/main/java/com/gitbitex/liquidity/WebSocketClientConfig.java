package com.gitbitex.liquidity;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;

import com.gitbitex.order.OrderManager;
import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketClientConfig {
    private final OrderManager orderManager;

    public static void main(String[] a) throws Exception, InterruptedException, IOException {
        //MyClient client1 = new MyClient(new URI("ws://gitbitex.cloud/ws"));
        //client1.connectBlocking();
    }

    //@PostConstruct
    public void init() {
        logger.info("start");
        Executors.newFixedThreadPool(1).execute(() -> {
            try {
                MyClient client1 = new MyClient(new URI("wss://ws-feed.exchange.coinbase.com"));
                client1.connectBlocking();
            } catch (Exception e) {
                logger.error("error", e);
            }
        });
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
        String productId = "BTC-USDT";
        String userId = "ad80fcfe-c3a1-46a1-acf8-1d6a909b2c5a";

        public MyClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            logger.info("open");

            send("{\"type\":\"subscribe\",\"product_ids\":[\"BTC-USDT\"],\"channels\":[\"full\"],\"token\":\"\"}");

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
                send(("{\"type\": \"ping\"}"));
            }, 0, 3, TimeUnit.SECONDS);
        }

        @Override
        public void onMessage(String s) {
            logger.info(s);
            try {
                ChannelMessage message = JSON.parseObject(s, ChannelMessage.class);
                switch (message.getType()) {
                    case "received":
                        if (message.getPrice() != null) {
                            orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId, OrderType.LIMIT,
                                OrderSide.valueOf(message.getSide().toUpperCase()), new BigDecimal(message.getSize()),
                                new BigDecimal(message.getPrice()), null, null, null);
                        }else{
                            orderManager.placeOrder(UUID.randomUUID().toString(), userId, productId, OrderType.MARKET,
                                OrderSide.valueOf(message.getSide().toUpperCase()), null,null,
                                new BigDecimal(message.getFunds()), null, null);
                        }
                        break;
                    case "done":
                        Order order=orderManager.findByOrderId(message.getOrderId());
                        if (order!=null){
                            orderManager.cancelOrder(order);
                        }
                        break;
                    default:
                }
            } catch (Exception e) {
                logger.error("error", e);
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {
            logger.error("error", e);
        }
    }
}
