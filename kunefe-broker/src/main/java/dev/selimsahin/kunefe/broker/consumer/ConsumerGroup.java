package dev.selimsahin.kunefe.broker.consumer;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a consumer group and its active members.
 * <p>
 * A consumer group ensures that each message is delivered to exactly one
 * member within the group. Multiple groups can consume the same topic
 * independently, each maintaining their own offset.
 */
public class ConsumerGroup {

    private final String groupId;
    private final String topic;
    private final Instant createdAt;
    private final Set<String> consumerIds;

    public ConsumerGroup(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
        this.createdAt = Instant.now();
        this.consumerIds = ConcurrentHashMap.newKeySet();
    }

    /**
     * Adds a consumer to this group.
     *
     * @return true if the consumer was added, false if already present
     */
    public boolean addConsumer(String consumerId) {
        return consumerIds.add(consumerId);
    }

    /**
     * Removes a consumer from this group.
     *
     * @return true if the consumer was removed, false if not found
     */
    public boolean removeConsumer(String consumerId) {
        return consumerIds.remove(consumerId);
    }

    public boolean hasConsumer(String consumerId) {
        return consumerIds.contains(consumerId);
    }

    public boolean isEmpty() {
        return consumerIds.isEmpty();
    }

    public Set<String> getConsumerIds() {
        return Collections.unmodifiableSet(consumerIds);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getTopic() {
        return topic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}