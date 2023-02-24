package com.gitbitex.order;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.log.Log;
import com.gitbitex.matchingengine.log.LogDispatcher;
import com.gitbitex.matchingengine.log.OrderDoneMessage;
import com.gitbitex.matchingengine.log.OrderDoneMessage.DoneReason;
import com.gitbitex.matchingengine.log.OrderFilledMessage;
import com.gitbitex.common.message.OrderMessage;
import com.gitbitex.common.message.OrderMessageHandler;
import com.gitbitex.matchingengine.log.OrderOpenMessage;
import com.gitbitex.matchingengine.log.OrderReceivedMessage;
import com.gitbitex.matchingengine.log.OrderRejectedMessage;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.order.entity.Order.OrderStatus;
import com.gitbitex.support.kafka.KafkaConsumerThread;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

@Slf4j
public class OrderPersistenceThread extends KafkaConsumerThread<String, Log>
    implements ConsumerRebalanceListener, OrderMessageHandler {
    private final OrderManager orderManager;
    private final KafkaMessageProducer messageProducer;
    private final AppProperties appProperties;
    private long uncommittedRecordCount;
    private final Map<Integer, Long> offsetByPartition = new HashMap<>();

    public OrderPersistenceThread(KafkaConsumer<String, Log> consumer, KafkaMessageProducer messageProducer,
        OrderManager orderManager, AppProperties appProperties) {
        super(consumer, logger);
        this.orderManager = orderManager;
        this.messageProducer = messageProducer;
        this.appProperties = appProperties;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        for (TopicPartition partition : partitions) {
            logger.info("partition revoked: {}", partition.toString());
            consumer.commitSync();
            //consumer.commitSync(offsetByPartition.entrySet().stream().map());
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
        consumer.subscribe(Collections.singletonList(appProperties.getOrderCommandTopic()), this);
    }

    @Override
    protected void doPoll() {
        var records = consumer.poll(Duration.ofSeconds(5));
        uncommittedRecordCount += records.count();

        for (ConsumerRecord<String, Log> record : records) {
            Log orderMessage = record.value();
            orderMessage.setOffset(record.offset());
            logger.info("{}", JSON.toJSONString(orderMessage));


            //offsetByPartition.put(record.partition(), record.offset());

            //consumer.commitSync();

            //consumer.commitSync(ImmutableMap.of(offset.getTopicPartition(), offset.getOffsetAndMetadata()));

        }

        if (uncommittedRecordCount > 10) {
            consumer.commitSync();
            uncommittedRecordCount = 0;
        }
    }

    @Override
    public void on(OrderReceivedMessage message) {
        orderManager.receiveOrder(message.getOrder());
    }

    @Override
    public void on(OrderRejectedMessage message) {
        orderManager.rejectOrder(message.getOrder());
    }

    @Override
    public void on(OrderOpenMessage message) {
        orderManager.openOrder(message.getOrderId());
    }

    @Override
    public void on(OrderDoneMessage message) {
        orderManager.closeOrder(message.getOrderId(),
            message.getDoneReason() == DoneReason.CANCELLED ? OrderStatus.CANCELLED : OrderStatus.FILLED);
    }

    @Override
    public void on(OrderFilledMessage command) {
        orderManager.fillOrder(command.getOrderId(), command.getTradeId(), command.getSize(), command.getPrice(),
            command.getFunds());
    }
}
