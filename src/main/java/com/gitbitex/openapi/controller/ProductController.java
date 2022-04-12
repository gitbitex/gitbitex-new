package com.gitbitex.openapi.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.CandleRepository;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import com.gitbitex.openapi.model.ProductDto;
import com.gitbitex.openapi.model.TradeDto;
import com.gitbitex.product.entity.Product;
import com.gitbitex.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
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

    @GetMapping("/api/products")
    public List<ProductDto> getProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::productDto).collect(Collectors.toList());
    }

    @GetMapping("/api/products/{productId}/trades")
    public List<TradeDto> getProductTrades(@PathVariable String productId) {
        List<Trade> trades = tradeRepository.findFirst50ByProductIdOrderByTimeDesc(productId);
        return trades.stream().map(this::tradeDto).collect(Collectors.toList());
    }

    @GetMapping("/api/products/{productId}/candles")
    public List<List<Object>> getProductCandles(@PathVariable String productId, @RequestParam int granularity,
        @RequestParam(defaultValue = "1000") int limit) {
        Page<Candle> candlePage = candleRepository.findAll(productId, granularity / 60, 1, limit);

        //[
        //    [ time, low, high, open, close, volume ],
        //    [ 1415398768, 0.32, 4.2, 0.35, 4.2, 12.3 ],
        //]
        List<List<Object>> lines = new ArrayList<>();
        candlePage.getContent().forEach(x -> {
            List<Object> line = new ArrayList<>();
            line.add(x.getTime());
            line.add(x.getLow());
            line.add(x.getHigh());
            line.add(x.getOpen());
            line.add(x.getClose());
            line.add(x.getVolume());
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
