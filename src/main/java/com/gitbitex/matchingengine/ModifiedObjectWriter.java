package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;
import com.gitbitex.kafka.KafkaMessageProducer;
import com.gitbitex.matchingengine.command.Command;
import com.gitbitex.matchingengine.message.OrderBookMessage;
import com.gitbitex.stripexecutor.StripedExecutorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ModifiedObjectWriter implements EngineListener {
    private final KafkaMessageProducer producer;
    private final StripedExecutorService kafkaExecutor = new StripedExecutorService(20);
    private final StripedExecutorService redisExecutor = new StripedExecutorService(20);
    private final Counter modifiedObjectCreatedCounter = Counter
            .builder("gbe.matching-engine.modified-object.created")
            .register(Metrics.globalRegistry);
    private final Counter modifiedObjectSavedCounter = Counter
            .builder("gbe.matching-engine.modified-object.saved")
            .register(Metrics.globalRegistry);
    private final RTopic accountTopic;
    private final RTopic orderTopic;
    private final RTopic orderBookMessageTopic;
    private final ConcurrentLinkedQueue<ModifiedObjectList> modifiedObjectLists = new ConcurrentLinkedQueue<>();

    public ModifiedObjectWriter(KafkaMessageProducer producer, RedissonClient redissonClient) {
        this.producer = producer;
        this.accountTopic = redissonClient.getTopic("account", StringCodec.INSTANCE);
        this.orderTopic = redissonClient.getTopic("order", StringCodec.INSTANCE);
        this.orderBookMessageTopic = redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE);
    }

    @Override
    public void onCommandExecuted(Command command, ModifiedObjectList modifiedObjects) {
        modifiedObjectLists.offer(modifiedObjects);
    }

    public void saveAsync(ModifiedObjectList modifiedObjects) {
        modifiedObjectCreatedCounter.increment(modifiedObjects.size());
        modifiedObjects.forEach(obj -> {
            if (obj instanceof Order) {
                save(modifiedObjects.getSavedCounter(), (Order) obj);
            } else if (obj instanceof Account) {
                save(modifiedObjects.getSavedCounter(), (Account) obj);
            } else if (obj instanceof Trade) {
                save(modifiedObjects.getSavedCounter(), (Trade) obj);
            } else if (obj instanceof OrderBookMessage) {
                save(modifiedObjects.getSavedCounter(), (OrderBookMessage) obj);
            } else {
                modifiedObjects.getSavedCounter().incrementAndGet();
                modifiedObjectSavedCounter.increment();
            }
        });
    }

    private void save(AtomicLong savedCounter, Account account) {
        kafkaExecutor.execute(account.getUserId(), () -> {
            String data = JSON.toJSONString(account);
            producer.sendAccount(account, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
            redisExecutor.execute(account.getUserId(), () -> {
                accountTopic.publishAsync(data);
            });
        });
    }

    private void save(AtomicLong savedCounter, Order order) {
        String productId = order.getProductId();
        kafkaExecutor.execute(productId, () -> {
            String data = JSON.toJSONString(order);
            producer.sendOrder(order, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
            redisExecutor.execute(order.getUserId(), () -> {
                orderTopic.publishAsync(data);
            });
        });
    }

    private void save(AtomicLong savedCounter, Trade trade) {
        kafkaExecutor.execute(trade.getProductId(), () -> {
            producer.sendTrade(trade, (m, e) -> {
                savedCounter.incrementAndGet();
                modifiedObjectSavedCounter.increment();
            });
        });
    }

    private void save(AtomicLong savedCounter, OrderBookMessage orderBookMessage) {
        savedCounter.incrementAndGet();
        modifiedObjectSavedCounter.increment();
        redisExecutor.execute(orderBookMessage.getProductId(), () -> {
            orderBookMessageTopic.publishAsync(JSON.toJSONString(orderBookMessage));
        });
    }


}
