package com.gitbitex.marketdata;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.gitbitex.AppProperties;
import com.gitbitex.kafka.TopicUtil;
import com.gitbitex.marketdata.entity.Trade;
import com.gitbitex.marketdata.repository.TradeRepository;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class TradePersistenceThread extends KafkaConsumerThread<String, OrderBookLog>
    implements ConsumerRebalanceListener {
    private final List<String> productIds;
    private final TradeRepository tradeRepository;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;

    public TradePersistenceThread(List<String> productIds, TradeRepository tradeRepository,
        KafkaConsumer<String, OrderBookLog> consumer, AppProperties appProperties) {
        super(consumer, logger);
        this.productIds = productIds;
        this.tradeRepository = tradeRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync();

        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition assigned: {}", partition.toString());
        }
    }

    @Override
    protected void doSubscribe() {
        List<String> topics = productIds.stream()
            .map(x -> TopicUtil.getProductTopic(x, appProperties.getOrderBookLogTopic()))
            .collect(Collectors.toList());
        consumer.subscribe(topics, this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, OrderBookLog> record : records) {
            OrderBookLog log = record.value();
            if (log instanceof OrderMatchLog) {
                OrderMatchLog orderMatchLog = ((OrderMatchLog)log);
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
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }
}
