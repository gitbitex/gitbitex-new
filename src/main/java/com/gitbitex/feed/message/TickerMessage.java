package com.gitbitex.feed.message;

import com.gitbitex.marketdata.entity.Ticker;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerMessage  {
    private String type;
    private String productId;
    private long tradeId;
    private long sequence;
    private long orderBookLogOffset;
    private String time;
    private String price;
    private String side;
    private String lastSize;
    private String open24h;
    private String close24h;
    private String high24h;
    private String low24h;
    private String volume24h;
    private String volume30d;

    public TickerMessage(Ticker ticker) {
        this.setType("ticker");
        this.setProductId(ticker.getProductId());
        this.setTradeId(ticker.getTradeId());
        this.setSequence(ticker.getSequence());
        this.setTime(ticker.getTime().toInstant().toString());
        this.setPrice(ticker.getPrice().toPlainString());
        this.setSide(ticker.getSide().name().toLowerCase());
        this.setLastSize(ticker.getLastSize().toPlainString());
        this.setClose24h(ticker.getClose24h().toPlainString());
        this.setOpen24h(ticker.getOpen24h().toPlainString());
        this.setHigh24h(ticker.getHigh24h().toPlainString());
        this.setLow24h(ticker.getLow24h().toPlainString());
        this.setVolume24h(ticker.getVolume24h().toPlainString());
        this.setVolume30d(ticker.getVolume30d().toPlainString());
    }

}
