package dev.selimsahin.kunefe.broker.topic;

import java.time.Instant;

/**
 * Represents a topic and its metadata.
 * <p>
 * Immutable by design - any change to a topic creates a new instance.
 * This prevents accidental mutation of topic state across threads.
 */
public record Topic(
        String name,
        int retentionHours,
        Instant createdAt,
        long messageCount
) {

    public static final int DEFAULT_RETENTION_HOURS = 24;

    public static Topic create(String name, int retentionHours) {
        return new Topic(name, retentionHours, Instant.now(), 0);
    }

    public static Topic withDefaults(String name) {
        return create(name, DEFAULT_RETENTION_HOURS);
    }

    public Topic incrementMessageCount() {
        return new Topic(name, retentionHours, createdAt, messageCount + 1);
    }
}