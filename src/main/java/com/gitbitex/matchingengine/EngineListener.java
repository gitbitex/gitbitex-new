package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.command.Command;


public interface EngineListener {
    /* private final KafkaMessageProducer producer;
     private final ModifiedObjectWriter modifiedObjectWriter;
     private final OrderBookSnapshotTaker orderBookSnapshotTaker;
     private final EngineSnapshotTaker engineSnapshotTaker;
     ExecutorService executorService = Executors.newFixedThreadPool(1);
 */
    void onCommandExecuted(Command command, ModifiedObjectList modifiedObjects);
}
