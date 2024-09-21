package com.gitbitex.matchingengine;

import com.gitbitex.matchingengine.snapshot.EngineSnapshotManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MatchingEngineLoader {
    private final EngineSnapshotManager engineSnapshotManager;
    private final MessageSender messageSender;
    @Getter
    @Nullable
    private volatile MatchingEngine preperedMatchingEngine;

    public MatchingEngineLoader(EngineSnapshotManager engineSnapshotManager, MessageSender messageSender) {
        this.engineSnapshotManager = engineSnapshotManager;
        this.messageSender = messageSender;
        startRefreshPreparingMatchingEnginePeriodically();
    }

    private void startRefreshPreparingMatchingEnginePeriodically() {
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
            try {
                logger.info("reloading latest snapshot");
                preperedMatchingEngine = new MatchingEngine(engineSnapshotManager, messageSender);
                logger.info("done");
            } catch (Exception e) {
                logger.error("matching engine create error: {}", e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

}
