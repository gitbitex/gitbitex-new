package com.gitbitex.fake;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;

import com.gitbitex.entity.Order.OrderSide;
import com.gitbitex.entity.Order.OrderType;
import com.gitbitex.module.order.OrderManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeMaker {
    private final OrderManager orderManager;
    @Value("${gbe.trade-maker-enabled:false}")
    private boolean tradeMakerEnabled;

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        if (!tradeMakerEnabled) {
            return;
        }

        Executors.newFixedThreadPool(1).submit(() -> {
            OkHttpClient httpClient = new OkHttpClient();

            String productId = "BTC-USDT";
            String userId = "ad80fcfe-c3a1-46a1-acf8-1d6a909b2c5a";

            long maxTradeId = 0;
            while (true) {
                try {
                    Request request = new Request.Builder()
                        .url("https://api.binance.com/api/v3/trades?symbol=BTCUSDT&limit=1")
                        .get()
                        .build();
                    Response response = httpClient.newCall(request).execute();
                    String responseBody = response.body().string();

                    List<Trade> trades = JSON.parseArray(responseBody, Trade.class);
                    for (Trade trade : trades) {
                        if (trade.getId() < maxTradeId) {
                            System.out.println("ignored");
                            continue;
                        }

                        maxTradeId = Long.max(maxTradeId, trade.getId());

                        System.out.println(JSON.toJSONString(trade));

                        BigDecimal size = new BigDecimal(trade.getQty());
                        BigDecimal price = new BigDecimal(trade.getPrice());

                        orderManager.placeOrder(userId, productId, OrderType.LIMIT,
                            OrderSide.BUY, size, price, null, null, null);
                        orderManager.placeOrder(userId, productId, OrderType.LIMIT,
                            OrderSide.SELL, size, price, null, null, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Thread.sleep(1000);
            }

        });
    }

    @Getter
    @Setter
    private static class Trade {
        private long id;
        private String price;
        private String qty;
        private boolean isBuyerMaker;
    }
}
