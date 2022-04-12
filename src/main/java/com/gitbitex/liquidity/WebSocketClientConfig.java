package com.gitbitex.liquidity;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class WebSocketClientConfig {

    public static class MyClient extends  org.java_websocket.client.WebSocketClient{

        public MyClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            logger.info("open");

            send("{\"type\":\"subscribe\",\"product_ids\":[\"BTC-USDT\"],\"channels\":[\"full\"],\"token\":\"\"}");

            Executors.newScheduledThreadPool(1).scheduleAtFixedRate(()->{
                send(("{\"type\": \"ping\"}"));
            },0,3,TimeUnit.SECONDS);
        }

        @Override
        public void onMessage(String s) {
            logger.info(s);
        }

        @Override
        public void onClose(int i, String s, boolean b) {

        }

        @Override
        public void onError(Exception e) {
            logger.error("error",e);
        }
    }

    public static void main(String[] a) throws Exception, InterruptedException, IOException {
        MyClient client1=new MyClient(new URI("ws://gitbitex.cloud/ws"));
        client1.connectBlocking();
    }

    //@PostConstruct
    public void init() {
        logger.info("start");
        Executors.newFixedThreadPool(1).execute(()->{
            try {
                MyClient client1=new MyClient(new URI("wss://ws-feed.exchange.coinbase.com"));
                client1.connectBlocking();
            } catch (Exception e) {
                logger.error("error",e);
            }
        });


    }
}
