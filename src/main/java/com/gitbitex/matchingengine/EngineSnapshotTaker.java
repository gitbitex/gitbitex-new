package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.Command;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EngineSnapshotTaker implements EngineListener {
    private final EngineSnapshotStore engineSnapshotStore;
    private final ConcurrentSkipListMap<Long, ModifiedObjectList> modifiedObjectsQueue = new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService mainExecutor = Executors.newScheduledThreadPool(1,
            new ThreadFactoryBuilder().setNameFormat("EngineSnapshotTaker-%s").build());
    private long lastCommandOffset;

    public EngineSnapshotTaker(EngineSnapshotStore engineSnapshotStore) {
        this.engineSnapshotStore = engineSnapshotStore;
        Gauge.builder("gbe_matching_engine_snapshot_taker_modified_objects_queue_size", modifiedObjectsQueue::size)
                .register(Metrics.globalRegistry);
        startMainTask();
    }

    @PreDestroy
    public void close() {
        this.mainExecutor.shutdown();
    }

    @Override
    public void onCommandExecuted(Command command, ModifiedObjectList modifiedObjects) {
        if (lastCommandOffset != 0 && modifiedObjects.getCommandOffset() <= lastCommandOffset) {
            logger.info("received processed message: {}", modifiedObjects.getCommandOffset());
            return;
        }
        lastCommandOffset = modifiedObjects.getCommandOffset();
        modifiedObjectsQueue.put(modifiedObjects.getCommandOffset(), modifiedObjects);
    }

    private void saveState() {
        if (modifiedObjectsQueue.isEmpty()) {
            return;
        }

        Long commandOffset = null;
        OrderBookState orderBookState = null;
        Map<String, Account> accounts = new HashMap<>();
        Map<String, Order> orders = new HashMap<>();
        Map<String, Product> products = new HashMap<>();
        for (Map.Entry<Long, ModifiedObjectList> entry : modifiedObjectsQueue.entrySet()) {
            ModifiedObjectList modifiedObjects = entry.getValue();
            if (!modifiedObjects.allSaved()) {
                break;
            }
            for (Object obj : modifiedObjects) {
                if (obj instanceof OrderBookState) {
                    orderBookState = (OrderBookState) obj;
                } else if (obj instanceof Account account) {
                    accounts.put(account.getId(), account);
                } else if (obj instanceof Order order) {
                    orders.put(order.getId(), order);
                } else if (obj instanceof Product product) {
                    products.put(product.getId(), product);
                }
            }
            commandOffset = entry.getKey();
        }

        if (commandOffset == null) {
            return;
        }

        Long savedCommandOffset = engineSnapshotStore.getCommandOffset();
        if (savedCommandOffset != null && commandOffset <= savedCommandOffset) {
            logger.warn("ignore outdated commandOffset: ignored={} saved={}", commandOffset, savedCommandOffset);
        } else {
            long t1 = System.currentTimeMillis();
            engineSnapshotStore.save(commandOffset, orderBookState, accounts.values(), orders.values(),
                    products.values());
            logger.info("state saved: commandOffset={}, {} account(s), {} order(s), {} product(s) ({}ms)", commandOffset,
                    accounts.size(), orders.size(), products.size(), System.currentTimeMillis() - t1);
        }

        Iterator<Map.Entry<Long, ModifiedObjectList>> itr = modifiedObjectsQueue.entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getKey() <= commandOffset) {
                itr.remove();
            }
        }
    }

    private void startMainTask() {
        mainExecutor.scheduleWithFixedDelay(() -> {
            try {
                saveState();
            } catch (Exception e) {
                logger.error("save state error: {}", e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
