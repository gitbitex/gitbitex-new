package com.gitbitex.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;

@Slf4j
public class MatchingEngineSnapshotThread extends MatchingEngineThread {
    private final OrderBookManager orderBookManager;
    private SafePointManager safePointManager;

    public MatchingEngineSnapshotThread(
        KafkaConsumer<String, MatchingEngineCommand> messageKafkaConsumer,
        OrderBookManager orderBookManager,
        AppProperties appProperties) {
        super(messageKafkaConsumer, null, appProperties);
        this.orderBookManager = orderBookManager;
    }

    @Override
    protected void doPoll() {
        super.doPoll();
        takeSnapshot();
    }

    private void takeSnapshot() {
        if (offset == lastSnapshotOffset) {
            logger.info("not yet");
            return;
        }

        //Long safePointCommandOffset = 5L;
        //if (offset <= safePointCommandOffset) {
        logger.info("take snapshot: offset={} lastSnapshotOffset={}", offset, lastSnapshotOffset);
        MatchingEngineSnapshot snapshot = matchingEngine.takeSnapshot();
        logger.info(JSON.toJSONString(snapshot, true));
        orderBookManager.saveFullOrderBookSnapshot(snapshot);
        lastSnapshotOffset = snapshot.getCommandOffset();
        lastSnapshotTime = snapshot.getTime();
        //}
    }
}
