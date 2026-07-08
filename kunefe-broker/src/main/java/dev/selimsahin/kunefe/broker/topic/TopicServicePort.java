package dev.selimsahin.kunefe.broker.topic;

import java.io.IOException;
import java.util.Collection;

/**
 * Port interface for topic service operations.
 * <p>
 * Abstracts topic lifecycle management behind an interface,
 * enabling dependency inversion and easier testing.
 */
public interface TopicServicePort {

    Topic createTopic(String name, int retentionHours) throws IOException;

    void deleteTopic(String name);

    Collection<Topic> listTopics();

    Topic getTopic(String name);

    boolean topicExists(String name);

    void incrementMessageCount(String name);
}