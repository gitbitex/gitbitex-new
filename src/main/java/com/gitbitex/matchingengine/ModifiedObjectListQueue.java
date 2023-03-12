package com.gitbitex.matchingengine;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ModifiedObjectListQueue {
    private final ConcurrentLinkedQueue<ModifiedObjectList> modifiedObjectsQueue = new ConcurrentLinkedQueue<>();
    private final AtomicLong sizeCounter = new AtomicLong();
    private final long maxSize;

    public ModifiedObjectListQueue(long maxSize) {
        this.maxSize = maxSize;
        Gauge.builder("gbe.matching-engine.modified-objects-queue.size", sizeCounter::get).register(Metrics.globalRegistry);
    }

    public void offer(ModifiedObjectList modifiedObjects) {
        while (sizeCounter.get() >= maxSize) {
            logger.warn("modified objects queue is full");
            Thread.yield();
        }
        sizeCounter.incrementAndGet();
        modifiedObjectsQueue.offer(modifiedObjects);
    }

    public ModifiedObjectList poll() {
        sizeCounter.decrementAndGet();
        return modifiedObjectsQueue.poll();
    }

    public long size() {
        return sizeCounter.get();
    }
}
