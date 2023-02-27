package com.gitbitex.matchingengine;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;

import com.gitbitex.marketdata.util.DateUtil;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.TickerMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;

@RequiredArgsConstructor
public class TickerBook {
    private final LogWriter logWriter;
    private final Map<String, Ticker> tickerByProductId = new HashMap<>();

    public void refreshTicker(String productId, OrderMatchLog log) {
        Ticker ticker = tickerByProductId.get(log.getProductId());
        if (ticker == null) {
            ticker = new Ticker();
            ticker.setProductId(log.getProductId());
        }

        long time24h = DateUtil.round(ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, 24 * 60).toEpochSecond();
        long time30d = DateUtil.round(ZonedDateTime.ofInstant(log.getTime().toInstant(), ZoneId.systemDefault()),
            ChronoField.MINUTE_OF_DAY, 24 * 60 * 30).toEpochSecond();

        if (ticker.getTime24h() == null || ticker.getTime24h() != time24h) {
            ticker.setTime24h(time24h);
            ticker.setOpen24h(log.getPrice());
            ticker.setClose24h(log.getPrice());
            ticker.setHigh24h(log.getPrice());
            ticker.setLow24h(log.getPrice());
            ticker.setVolume24h(log.getSize());
        } else {
            ticker.setClose24h(log.getPrice());
            ticker.setHigh24h(ticker.getHigh24h().max(log.getPrice()));
            ticker.setVolume24h(ticker.getVolume24h().add(log.getSize()));
        }
        if (ticker.getTime30d() == null || ticker.getTime30d() != time30d) {
            ticker.setTime30d(time30d);
            ticker.setOpen30d(log.getPrice());
            ticker.setClose30d(log.getPrice());
            ticker.setHigh30d(log.getPrice());
            ticker.setLow30d(log.getPrice());
            ticker.setVolume30d(log.getSize());
        } else {
            ticker.setClose30d(log.getPrice());
            ticker.setHigh30d(ticker.getHigh30d().max(log.getPrice()));
            ticker.setVolume30d(ticker.getVolume30d().add(log.getSize()));
        }
        ticker.setLastSize(log.getSize());
        ticker.setTime(log.getTime());
        ticker.setPrice(log.getPrice());
        ticker.setSide(log.getSide());
        ticker.setTradeId(log.getTradeId());
        ticker.setSequence(log.getSequence());
        tickerByProductId.put(productId, ticker);

        sendTickerMessage(ticker);
    }

    private void sendTickerMessage(Ticker ticker) {
        TickerMessage tickerMessage = new TickerMessage();
        BeanUtils.copyProperties(ticker, tickerMessage);
        logWriter.add(tickerMessage);
    }

}
