package dev.selimsahin.kunefe.broker.topic;

import java.util.Collection;
import java.util.Optional;

/**
 * Port interface for topic registry operations.
 * <p>
 * Abstracts the in-memory topic registry behind an interface,
 * enabling dependency inversion and easier testing.
 */
public interface TopicRegistryPort {

    void register(Topic topic);

    void update(Topic topic);

    Optional<Topic> find(String name);

    boolean exists(String name);

    void remove(String name);

    Collection<Topic> findAll();
}