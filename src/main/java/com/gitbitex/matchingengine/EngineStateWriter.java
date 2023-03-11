package com.gitbitex.matchingengine;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EngineStateWriter {
    private final EngineStateStore engineStateStore;
    private final ConcurrentSkipListMap<Long, ModifiedObjectList> modifiedObjects =
            new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    public EngineStateWriter(EngineStateStore engineStateStore) {
        this.engineStateStore = engineStateStore;
        Gauge.builder("gbe.matching-engine.state-unsaved-modified-object-map.size", modifiedObjects::size)
                .register(Metrics.globalRegistry);
        startSateSaveTask();
    }

    public void append(ModifiedObjectList modifiedObjects) {
        this.modifiedObjects.put(modifiedObjects.getCommandOffset(), modifiedObjects);
    }

    private void saveState() {
        if (modifiedObjects.isEmpty()) {
            return;
        }

        Long commandOffset = null;
        OrderBookState orderBookState = null;
        Map<String, Account> accounts = new HashMap<>();
        Map<String, Order> orders = new HashMap<>();
        Map<String, Product> products = new HashMap<>();
        for (Map.Entry<Long, ModifiedObjectList> entry : modifiedObjects.entrySet()) {
            ModifiedObjectList modifiedObjects = entry.getValue();
            if (!modifiedObjects.allSaved()) {
                break;
            }
            for (Object obj : modifiedObjects) {
                if (obj instanceof OrderBookState) {
                    orderBookState = (OrderBookState) obj;
                } else if (obj instanceof Account) {
                    Account account = (Account) obj;
                    accounts.put(account.getId(), account);
                } else if (obj instanceof Order) {
                    Order order = (Order) obj;
                    orders.put(order.getId(), order);
                } else if (obj instanceof Product) {
                    Product product = (Product) obj;
                    products.put(product.getId(), product);
                }
            }
            commandOffset = entry.getKey();
        }

        if (commandOffset == null) {
            return;
        }
        Long savedCommandOffset = engineStateStore.getCommandOffset();
        if (savedCommandOffset != null && commandOffset <= savedCommandOffset) {
            logger.warn("ignore outdated commandOffset: ignored={} saved={}", commandOffset, savedCommandOffset);
            return;
        }
        engineStateStore.save(commandOffset, orderBookState, accounts.values(), orders.values(), products.values());
        logger.info("state saved: commandOffset={}, {} account(s), {} order(s), {} product(s)", commandOffset,
                accounts.size(), orders.size(), products.size());


        Iterator<Map.Entry<Long, ModifiedObjectList>> itr = modifiedObjects.entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getKey() <= commandOffset) {
                itr.remove();
            }
        }
    }

    private void startSateSaveTask() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                saveState();
            } catch (Exception e) {
                logger.error("save state error: {}", e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
