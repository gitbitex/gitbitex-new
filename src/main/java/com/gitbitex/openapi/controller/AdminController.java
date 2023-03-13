package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.Product;
import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.manager.AccountManager;
import com.gitbitex.marketdata.manager.UserManager;
import com.gitbitex.marketdata.repository.ProductRepository;
import com.gitbitex.matchingengine.command.CancelOrderCommand;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandProducer;
import com.gitbitex.matchingengine.command.PutProductCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * For demonstration, do not expose to external users ！！！！！！
 * For demonstration, do not expose to external users ！！！！！！
 * For demonstration, do not expose to external users ！！！！！！
 */
@RestController
@RequiredArgsConstructor
public class AdminController {
    private final MatchingEngineCommandProducer producer;
    private final AccountManager accountManager;
    private final ProductRepository productRepository;
    private final UserManager userManager;

    @GetMapping("/api/admin/createUser")
    public User createUser(String email, String password) {
        User user = userManager.getUser(email, password);
        if (user != null) {
            return user;
        }
        return userManager.createUser(email, password);
    }

    @GetMapping("/api/admin/deposit")
    public String deposit(@RequestParam String userId, @RequestParam String currency, @RequestParam String amount) {
        DepositCommand command = new DepositCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(new BigDecimal(amount));
        command.setTransactionId(UUID.randomUUID().toString());
        producer.send(command, null);
        return "ok";
    }

    @PutMapping("/api/admin/products")
    public Product saveProduct(@RequestBody @Valid PutProductRequest request) {
        String productId = request.getBaseCurrency() + "-" + request.getQuoteCurrency();
        Product product = new Product();
        product.setId(productId);
        product.setBaseCurrency(request.baseCurrency);
        product.setQuoteCurrency(request.quoteCurrency);
        product.setBaseScale(6);
        product.setQuoteScale(2);
        product.setBaseMinSize(BigDecimal.ZERO);
        product.setBaseMaxSize(new BigDecimal("100000000"));
        product.setQuoteMinSize(BigDecimal.ZERO);
        product.setQuoteMaxSize(new BigDecimal("10000000000"));
        productRepository.save(product);

        PutProductCommand putProductCommand = new PutProductCommand();
        putProductCommand.setProductId(product.getId());
        putProductCommand.setBaseCurrency(product.getBaseCurrency());
        putProductCommand.setQuoteCurrency(product.getQuoteCurrency());
        producer.send(putProductCommand, null);

        return product;
    }

    public void cancelOrder(String orderId, String productId) {
        CancelOrderCommand command = new CancelOrderCommand();
        command.setProductId(productId);
        command.setOrderId(orderId);
        producer.send(command, null);
    }

    @Getter
    @Setter
    public static class PutProductRequest {
        @NotBlank
        private String baseCurrency;
        @NotBlank
        private String quoteCurrency;

    }

}
