package dev.selimsahin.kunefe.broker.consumer;

import java.util.Collection;
import java.util.Optional;

/**
 * Port interface for consumer group registry operations.
 */
public interface ConsumerGroupRegistryPort {

    ConsumerGroup register(String groupId, String topic);

    Optional<ConsumerGroup> find(String groupId, String topic);

    boolean exists(String groupId, String topic);

    void remove(String groupId, String topic);

    Collection<ConsumerGroup> findAll();
}