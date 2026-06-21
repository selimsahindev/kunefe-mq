package dev.selimsahin.kunefe.broker.topic;

/**
 * Thrown when a requested topic does not exist.
 */
public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException(String topic) {
        super("Topic not found: " + topic);
    }
}