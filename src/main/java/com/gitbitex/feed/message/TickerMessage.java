package com.gitbitex.feed.message;

import com.gitbitex.marketdata.entity.Ticker;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TickerMessage {
    private String type = "ticker";
    private String productId;
    private long tradeId;
    private long sequence;
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
        this.setProductId(ticker.getProductId());
        this.setTradeId(ticker.getTradeId());
        this.setSequence(ticker.getSequence());
        this.setTime(ticker.getTime().toInstant().toString());
        this.setPrice(ticker.getPrice().stripTrailingZeros().toPlainString());
        this.setSide(ticker.getSide().name().toLowerCase());
        this.setLastSize(ticker.getLastSize().stripTrailingZeros().toPlainString());
        this.setClose24h(ticker.getClose24h().stripTrailingZeros().toPlainString());
        this.setOpen24h(ticker.getOpen24h().stripTrailingZeros().toPlainString());
        this.setHigh24h(ticker.getHigh24h().stripTrailingZeros().toPlainString());
        this.setLow24h(ticker.getLow24h().stripTrailingZeros().toPlainString());
        this.setVolume24h(ticker.getVolume24h().stripTrailingZeros().toPlainString());
        this.setVolume30d(ticker.getVolume30d().stripTrailingZeros().toPlainString());
    }

}
