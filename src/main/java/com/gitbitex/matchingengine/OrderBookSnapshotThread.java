package com.gitbitex.matchingengine;

import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.snapshot.L2OrderBook;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class OrderBookSnapshotThread extends MatchingEngineThread {
    private final OrderBookManager orderBookManager;
    private ConcurrentHashMap<String, Long> lastL2OrderBookVersions = new ConcurrentHashMap<>();

    public OrderBookSnapshotThread(KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer,
        OrderBookManager orderBookManager, AppProperties appProperties) {
        super(messageKafkaConsumer, null, appProperties);
        this.orderBookManager=orderBookManager;
    }

    @Override
    protected void doPoll() {
        super.doPoll();
        takeSnapshot();
    }

    private void takeSnapshot() {
        matchingEngine.getOrderBooks().keySet().forEach(x -> {
            Long lastL2OrderBookVersion = lastL2OrderBookVersions.get(x);
            if (lastL2OrderBookVersion != null && lastL2OrderBookVersion == offset) {
                logger.info("nothing changed: {}", x);
                return;
            }
            logger.info("take l2 order book snapshot: offset={} lastSnapshotOffset={}", offset, lastSnapshotOffset);
            L2OrderBook l2OrderBook = matchingEngine.takeL2OrderBookSnapshot(x, 10);
            logger.info(JSON.toJSONString(l2OrderBook, true));
            orderBookManager.saveL2BatchOrderBook(l2OrderBook);
            lastL2OrderBookVersions.put(x,offset);
        });
    }
}
