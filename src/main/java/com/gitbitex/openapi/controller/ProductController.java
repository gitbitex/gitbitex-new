package com.gitbitex.openapi.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Product;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.manager.UserManager;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.ProductRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.command.PutProductCommand;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.openapi.model.PagedList;
import com.gitbitex.openapi.model.ProductDto;
import com.gitbitex.openapi.model.TradeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequiredArgsConstructor
public class ProductController {
    private final OrderBookManager orderBookManager;
    private final ProductRepository productRepository;
    private final TradeRepository tradeRepository;
    private final CandleRepository candleRepository;
    private final UserManager userManager;
    private final KafkaMessageProducer producer;

    @GetMapping("/api/admin/addProduct")
    public void addProduct(@RequestParam String baseCurrency, @RequestParam String quoteCurrency) {
        Product product = new Product();
        product.setProductId(baseCurrency + "-" + quoteCurrency);
        product.setBaseCurrency(baseCurrency);
        product.setQuoteCurrency(quoteCurrency);
        product.setBaseScale(6);
        product.setQuoteScale(2);
        product.setBaseMinSize(BigDecimal.ZERO);
        product.setBaseMaxSize(new BigDecimal("100000000"));
        productRepository.save(product);

        PutProductCommand putProductCommand = new PutProductCommand();
        putProductCommand.setProductId(product.getProductId());
        putProductCommand.setBaseCurrency(product.getBaseCurrency());
        putProductCommand.setQuoteCurrency(product.getQuoteCurrency());
        producer.sendToMatchingEngine("all", putProductCommand, null);
    }

    @GetMapping("/api/admin/addUser")
    public void addUser(@RequestParam String email, @RequestParam String password) {
        User user = new User();
        userManager.createUser(email, password);
    }

    @GetMapping("/api/products")
    public List<ProductDto> getProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::productDto).collect(Collectors.toList());
    }

    @GetMapping("/api/products/{productId}/trades")
    public List<TradeDto> getProductTrades(@PathVariable String productId) {
        List<Trade> trades = tradeRepository.findTradesByProductId(productId, 50);
        return trades.stream().map(this::tradeDto).collect(Collectors.toList());
    }

    @GetMapping("/api/products/{productId}/candles")
    public List<List<Object>> getProductCandles(@PathVariable String productId, @RequestParam int granularity,
        @RequestParam(defaultValue = "1000") int limit) {
        PagedList<Candle> candlePage = candleRepository.findAll(productId, granularity / 60, 1, limit);

        //[
        //    [ time, low, high, open, close, volume ],
        //    [ 1415398768, 0.32, 4.2, 0.35, 4.2, 12.3 ],
        //]
        List<List<Object>> lines = new ArrayList<>();
        candlePage.getItems().forEach(x -> {
            List<Object> line = new ArrayList<>();
            line.add(x.getTime());
            line.add(x.getLow().stripTrailingZeros());
            line.add(x.getHigh().stripTrailingZeros());
            line.add(x.getOpen().stripTrailingZeros());
            line.add(x.getClose().stripTrailingZeros());
            line.add(x.getVolume().stripTrailingZeros());
            lines.add(line);
        });
        return lines;
    }

    @GetMapping("/api/products/{productId}/book")
    public Object getProductBook(@PathVariable String productId, @RequestParam(defaultValue = "2") int level) {
        switch (level) {
            case 1:
                return orderBookManager.getL1OrderBook(productId);
            case 2:
                return orderBookManager.getL2OrderBook(productId);
            case 3:
                return orderBookManager.getL3OrderBook(productId);
            default:
                return null;
        }
    }

    private ProductDto productDto(Product product) {
        ProductDto productDto = new ProductDto();
        BeanUtils.copyProperties(product, productDto);
        productDto.setId(product.getProductId());
        productDto.setQuoteIncrement(String.valueOf(product.getQuoteIncrement()));
        return productDto;
    }

    private TradeDto tradeDto(Trade trade) {
        TradeDto tradeDto = new TradeDto();
        tradeDto.setTradeId(trade.getTradeId());
        tradeDto.setTime(trade.getTime().toInstant().toString());
        tradeDto.setPrice(trade.getPrice().toPlainString());
        tradeDto.setSize(trade.getSize().toPlainString());
        tradeDto.setSide(trade.getSide().name().toLowerCase());
        return tradeDto;
    }
}
