package dev.selimsahin.kunefe.broker.consumer;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of all active consumer groups.
 * <p>
 * Keyed by a composite key of groupId and topic to allow the same
 * group to consume multiple topics independently.
 */
@Component
public class ConsumerGroupRegistry {

    private final ConcurrentHashMap<String, ConsumerGroup> groups = new ConcurrentHashMap<>();

    public ConsumerGroup register(String groupId, String topic) {
        String key = buildKey(groupId, topic);
        return groups.computeIfAbsent(key, k -> new ConsumerGroup(groupId, topic));
    }

    public Optional<ConsumerGroup> find(String groupId, String topic) {
        return Optional.ofNullable(groups.get(buildKey(groupId, topic)));
    }

    public boolean exists(String groupId, String topic) {
        return groups.containsKey(buildKey(groupId, topic));
    }

    public void remove(String groupId, String topic) {
        groups.remove(buildKey(groupId, topic));
    }

    public Collection<ConsumerGroup> findAll() {
        return Collections.unmodifiableCollection(groups.values());
    }

    /**
     * Composite key ensures group-topic uniqueness.
     * Example: "payments-group::orders"
     */
    private String buildKey(String groupId, String topic) {
        return groupId + "::" + topic;
    }
}