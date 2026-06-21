package dev.selimsahin.kunefe.broker.topic;

import dev.selimsahin.kunefe.broker.log.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;

/**
 * Manages topic lifecycle and enforces business rules.
 * <p>
 * Acts as the single entry point for any topic-related operation.
 * Coordinates between TopicRegistry (metadata) and LogManager (data).
 */
@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);

    private final TopicRegistry topicRegistry;
    private final LogManager logManager;

    public TopicService(TopicRegistry topicRegistry, LogManager logManager) {
        this.topicRegistry = topicRegistry;
        this.logManager = logManager;
    }

    /**
     * Creates a new topic with the given retention policy.
     * Throws if the topic already exists.
     */
    public Topic createTopic(String name, int retentionHours) throws IOException {
        if (topicRegistry.exists(name)) {
            throw new TopicAlreadyExistsException(name);
        }

        Topic topic = Topic.create(name, retentionHours);
        topicRegistry.register(topic);
        logManager.createTopic(name);

        log.info("Topic '{}' created with retention {} hours", name, retentionHours);
        return topic;
    }

    /**
     * Deletes a topic and its associated log.
     * Throws if the topic does not exist.
     */
    public void deleteTopic(String name) {
        if (!topicRegistry.exists(name)) {
            throw new TopicNotFoundException(name);
        }

        topicRegistry.remove(name);
        log.info("Topic '{}' deleted", name);
    }

    /**
     * Returns all registered topics.
     */
    public Collection<Topic> listTopics() {
        return topicRegistry.findAll();
    }

    /**
     * Returns a topic by name.
     * Throws if the topic does not exist.
     */
    public Topic getTopic(String name) {
        return topicRegistry.find(name)
                .orElseThrow(() -> new TopicNotFoundException(name));
    }

    /**
     * Checks whether a topic exists.
     */
    public boolean topicExists(String name) {
        return topicRegistry.exists(name);
    }

    /**
     * Increments the message count metadata for a topic.
     * Called by ProducerService after each successful publish.
     */
    public void incrementMessageCount(String name) {
        topicRegistry.find(name).ifPresent(topic -> {
            topicRegistry.update(topic.incrementMessageCount());
        });
    }
}