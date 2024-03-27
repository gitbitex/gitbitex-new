package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.TradeEntity;
import com.gitbitex.marketdata.manager.TradeManager;
import com.gitbitex.matchingengine.MessageConsumerThread;
import com.gitbitex.matchingengine.message.Message;
import com.gitbitex.matchingengine.message.TradeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TradePersistenceThread extends MessageConsumerThread {
    private final TradeManager tradeManager;
    private final AppProperties appProperties;

    public TradePersistenceThread(KafkaConsumer<String, Message> consumer, TradeManager tradeManager,
                                  AppProperties appProperties) {
        super(consumer, appProperties, logger);
        this.tradeManager = tradeManager;
        this.appProperties = appProperties;
    }

    @Override
    protected void processRecords(ConsumerRecords<String, Message> records) {
        Map<String, TradeEntity> trades = new HashMap<>();
        records.forEach(x -> {
            Message message = x.value();
            if (message instanceof TradeMessage) {
                TradeEntity trade = trade((TradeMessage) message);
                trades.put(trade.getId(), trade);
            }
        });

        tradeManager.saveAll(trades.values());
    }

    private TradeEntity trade(TradeMessage message) {
        TradeEntity trade = new TradeEntity();
        trade.setId(message.getTrade().getProductId() + "-" + message.getTrade().getSequence());
        trade.setSequence(message.getTrade().getSequence());
        trade.setTime(message.getTrade().getTime());
        trade.setSize(message.getTrade().getSize());
        trade.setPrice(message.getTrade().getPrice());
        trade.setProductId(message.getTrade().getProductId());
        trade.setMakerOrderId(message.getTrade().getMakerOrderId());
        trade.setTakerOrderId(message.getTrade().getTakerOrderId());
        trade.setSide(message.getTrade().getSide());
        return trade;
    }

}
