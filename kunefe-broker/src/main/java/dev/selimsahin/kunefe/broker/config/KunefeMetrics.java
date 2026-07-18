package dev.selimsahin.kunefe.broker.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Custom metrics for Kunefe MQ broker.
 * <p>
 * Exposes topic-level and consumer-level metrics via Micrometer,
 * which are scraped by Prometheus and visualized in Grafana.
 */
@Component
public class KunefeMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> publishCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> consumeCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> consumerLags = new ConcurrentHashMap<>();
    private final AtomicInteger activeConsumers = new AtomicInteger(0);

    public KunefeMetrics(MeterRegistry registry) {
        this.registry = registry;
        registerActiveConsumersGauge();
    }

    /**
     * Records a published message for the given topic.
     */
    public void recordPublish(String topic) {
        publishCounters.computeIfAbsent(topic, t ->
                Counter.builder("kunefe.messages.published")
                        .tag("topic", t)
                        .description("Total number of messages published to a topic")
                        .register(registry)
        ).increment();
    }

    /**
     * Records a consumed message for the given topic and consumer group.
     */
    public void recordConsume(String topic, String consumerGroup) {
        String key = topic + "::" + consumerGroup;
        consumeCounters.computeIfAbsent(key, k ->
                Counter.builder("kunefe.messages.consumed")
                        .tag("topic", topic)
                        .tag("consumer_group", consumerGroup)
                        .description("Total number of messages consumed from a topic")
                        .register(registry)
        ).increment();
    }

    /**
     * Updates the consumer lag for a given topic and consumer group.
     * Lag = broker's next offset - consumer's current offset
     */
    public void updateConsumerLag(String topic, String consumerGroup, long lag) {
        String key = topic + "::" + consumerGroup;
        AtomicLong lagValue = consumerLags.computeIfAbsent(key, k -> {
            AtomicLong value = new AtomicLong(0);
            Gauge.builder("kunefe.consumer.lag", value, AtomicLong::get)
                    .tag("topic", topic)
                    .tag("consumer_group", consumerGroup)
                    .description("Number of messages the consumer is behind the broker")
                    .register(registry);
            return value;
        });
        lagValue.set(lag);
    }

    /**
     * Increments the active consumer count when a consumer subscribes.
     */
    public void incrementActiveConsumers() {
        activeConsumers.incrementAndGet();
    }

    /**
     * Decrements the active consumer count when a consumer unsubscribes.
     */
    public void decrementActiveConsumers() {
        activeConsumers.decrementAndGet();
    }

    /**
     * Registers a gauge for tracking active consumer count.
     */
    private void registerActiveConsumersGauge() {
        Gauge.builder("kunefe.active.consumers", activeConsumers, AtomicInteger::get)
                .description("Number of currently active consumers")
                .register(registry);
    }

    /**
     * Registers a gauge for segment count of a topic.
     * Called by TopicLog when a new segment is created.
     */
    public void registerSegmentCountGauge(String topic, Supplier<Integer> segmentCountSupplier) {
        Gauge.builder("kunefe.segments.total", segmentCountSupplier, Supplier::get)
                .tag("topic", topic)
                .description("Total number of log segments for a topic")
                .register(registry);
    }
}