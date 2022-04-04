package com.gitbitex.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html
 *
 * @param <K>
 * @param <V>
 */
@RequiredArgsConstructor
public abstract class KafkaConsumerThread<K, V> extends Thread {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final KafkaConsumer<K, V> consumer;
    private final Logger logger;

    @Override
    public void run() {
        logger.info("starting...");
        try {
            // subscribe
            doSubscribe(consumer);
            consumer.subscription().forEach(x -> {
                logger.info("subscribing topic: {}", x);
            });

            // poll & process
            while (!closed.get()) {
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofSeconds(10));
                processRecords(consumer, records);
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

    protected abstract void doSubscribe(KafkaConsumer<K, V> consumer);

    protected abstract void processRecords(KafkaConsumer<K, V> consumer, ConsumerRecords<K, V> records);
}
