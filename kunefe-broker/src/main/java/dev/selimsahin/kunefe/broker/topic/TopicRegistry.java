package dev.selimsahin.kunefe.broker.topic;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of all known topics and their metadata.
 * <p>
 * Intentionally kept as a thin, stateful component with no business logic.
 * All business rules live in TopicService.
 */
@Component
public class TopicRegistry {

    private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();

    public void register(Topic topic) {
        topics.put(topic.name(), topic);
    }

    public void update(Topic topic) {
        topics.replace(topic.name(), topic);
    }

    public Optional<Topic> find(String name) {
        return Optional.ofNullable(topics.get(name));
    }

    public boolean exists(String name) {
        return topics.containsKey(name);
    }

    public void remove(String name) {
        topics.remove(name);
    }

    public Collection<Topic> findAll() {
        return Collections.unmodifiableCollection(topics.values());
    }
}