package com.gitbitex.middleware.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html
 * <p>
 * Multi-threaded Processing
 * The Kafka consumer is NOT thread-safe. All network I/O happens in the thread of the application making the call.
 * It is the responsibility of the user to ensure that multi-threaded access is properly synchronized.
 * Un-synchronized access will result in ConcurrentModificationException.
 * The only exception to this rule is wakeup(), which can safely be used from an external thread to interrupt an
 * active operation. In this case, a WakeupException will be thrown from the thread blocking on the operation. This
 * can be used to shutdown the consumer from another thread. The following snippet shows the typical pattern:
 *
 * @param <K>
 * @param <V>
 */
@RequiredArgsConstructor
public abstract class KafkaConsumerThread<K, V> extends Thread {
    protected final KafkaConsumer<K, V> consumer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Logger logger;

    @Override
    public void run() {
        logger.info("starting...");
        try {
            // subscribe
            doSubscribe();

            // poll & process
            while (!closed.get()) {
                doPoll();
            }
        } catch (WakeupException e) {
            // ignore exception if closing
            if (!closed.get()) {
                throw e;
            }
        } catch (Exception e) {
            logger.error("consumer error: {}", e.getMessage(), e);
        } finally {
            consumer.close();
        }
        logger.info("exiting...");
    }

    public void shutdown() {
        closed.set(true);
        consumer.wakeup();
    }

    @Override
    public void interrupt() {
        this.shutdown();
        super.interrupt();
    }

    protected abstract void doSubscribe();

    protected abstract void doPoll();
}
