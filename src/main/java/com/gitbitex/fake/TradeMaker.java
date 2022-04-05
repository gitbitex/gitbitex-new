package com.gitbitex.fake;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Component
public class TradeMaker {

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        Executors.newFixedThreadPool(1).submit(() -> {
            OkHttpClient httpClient = new OkHttpClient();

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
                        placeOrder(httpClient, "BTC-USDT", trade.getPrice(), trade.getQty(),
                                trade.isBuyerMaker ? "buy" : "sell");
                        placeOrder(httpClient, "BTC-USDT", trade.getPrice(), trade.getQty(),
                                trade.isBuyerMaker ? "sell" : "buy");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Thread.sleep(1000);
            }

        });
    }

    private void placeOrder(OkHttpClient httpClient, String productId, String price, String size, String side)
            throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("productId", productId);
        params.put("price", price);
        params.put("size", size);
        params.put("side", side);
        params.put("type", "limit");
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), JSON.toJSONString(params));
        Request request = new Request.Builder()
                .url(
                        "http://127.0.0.1:4567/api/orders?accessToken=ad80fcfe-c3a1-46a1-acf8-1d6a909b2c5a:567AFF91EB9D0AEA362F25286B0FD9C8:5e221aa93950f87135672ca84b82e3bf")
                .post(requestBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        String responseBody = response.body().string();
        System.out.println(responseBody);
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
