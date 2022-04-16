package com.gitbitex.marketdata;

import com.gitbitex.AppProperties;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, OrderBookLog> {
    private final String productId;
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public TradePersistenceThread(String productId, TradeRepository tradeRepository,
                                  KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productId = productId;
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
    }

    @Override
    protected void doSubscribe() {
        consumer.subscribe(Collections.singletonList(productId + "-" + appProperties.getOrderBookLogTopic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                consumer.commitSync();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {

            }
        });
    }

    @Override
    protected void processRecords(ConsumerRecords<String, OrderBookLog> records) {
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            if (log instanceof OrderMatchLog) {
                OrderMatchLog orderMatchLog = ((OrderMatchLog) log);
                orderMatchLog.setOffset(record.offset());
                Trade trade = tradeRepository.findByProductIdAndTradeId(orderMatchLog.getProductId(),
                        orderMatchLog.getTradeId());
                if (trade == null) {
                    trade = new Trade();
                    trade.setTradeId(orderMatchLog.getTradeId());
                    trade.setTime(orderMatchLog.getTime());
                    trade.setSize(orderMatchLog.getSize());
                    trade.setPrice(orderMatchLog.getPrice());
                    trade.setProductId(orderMatchLog.getProductId());
                    trade.setMakerOrderId(orderMatchLog.getMakerOrderId());
                    trade.setTakerOrderId(orderMatchLog.getTakerOrderId());
                    trade.setSide(orderMatchLog.getSide());
                    trade.setSequence(log.getSequence());
                    trade.setOrderBookLogOffset(record.offset());
                    tradeRepository.save(trade);
                }
            }
        }

        if (uncommittedRecordCount > 1000) {
            consumer.commitAsync();
            uncommittedRecordCount = 0;
        }
    }
}
