package dev.selimsahin.kunefe.broker.topic;

/**
 * Thrown when attempting to create a topic that already exists.
 */
public class TopicAlreadyExistsException extends RuntimeException {

    public TopicAlreadyExistsException(String topic) {
        super("Topic already exists: " + topic);
    }
}