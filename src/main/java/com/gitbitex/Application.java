package com.gitbitex;

import com.gitbitex.matchingengine.AccountBook;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.FastMoney;
import org.javamoney.moneta.Money;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.convert.MonetaryConversions;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        /*long     t1=System.currentTimeMillis();

        String userId =  UUID.randomUUID().toString();
        //Long userId=1L;
        Map<String, String> map = new HashMap<>();
        map.put(userId,"userId");
        for (int i = 0; i < 1000000; i++) {
            System.out.println(map.get(userId));
        }
        System.out.println(System.currentTimeMillis()-t1);
if (true)return;*/
        SpringApplication.run(Application.class, args);
    }

}
