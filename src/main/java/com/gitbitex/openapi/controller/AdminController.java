package com.gitbitex.openapi.controller;

import java.math.BigDecimal;
import java.util.UUID;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Product;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.marketdata.repository.ProductRepository;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.PutProductCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {
    private final KafkaMessageProducer producer;
    private final AccountManager accountManager;
    private final ProductRepository productRepository;

    @GetMapping("/api/admin/deposit")
    public String deposit(@RequestParam String userId, @RequestParam String currency, @RequestParam String amount) {
        DepositCommand command = new DepositCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(new BigDecimal(amount));
        command.setTransactionId(UUID.randomUUID().toString());
        producer.sendToMatchingEngine("all", command, null);
        return "ok";
    }

    @GetMapping("/api/admin/addProduct")
    public void saveProduct(@RequestParam String baseCurrency, @RequestParam String quoteCurrency) {
        String productId = baseCurrency + "-" + quoteCurrency;
        Product product = new Product();
        product.setId(productId);
        product.setBaseCurrency(baseCurrency);
        product.setQuoteCurrency(quoteCurrency);
        product.setBaseScale(6);
        product.setQuoteScale(2);
        product.setBaseMinSize(BigDecimal.ZERO);
        product.setBaseMaxSize(new BigDecimal("100000000"));
        productRepository.save(product);

        PutProductCommand putProductCommand = new PutProductCommand();
        putProductCommand.setProductId(product.getId());
        putProductCommand.setBaseCurrency(product.getBaseCurrency());
        putProductCommand.setQuoteCurrency(product.getQuoteCurrency());
        producer.sendToMatchingEngine("all", putProductCommand, null);
    }
}
