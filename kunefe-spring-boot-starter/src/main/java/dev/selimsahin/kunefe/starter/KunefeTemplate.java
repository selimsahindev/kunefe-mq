package dev.selimsahin.kunefe.starter;

import dev.selimsahin.kunefe.client.KunefeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Primary API for publishing messages to Kunefe broker from Spring applications.
 * <p>
 * Analogous to Spring's KafkaTemplate — provides a simple, Spring-friendly
 * interface for message publishing without exposing gRPC internals.
 * <p>
 * Registered as a Spring bean by KunefeAutoConfiguration.
 */
public class KunefeTemplate {

    private static final Logger log = LoggerFactory.getLogger(KunefeTemplate.class);

    private final KunefeClient client;

    public KunefeTemplate(KunefeClient client) {
        this.client = client;
    }

    /**
     * Publishes a raw byte array to the given topic.
     *
     * @param topic   the target topic name
     * @param payload the raw message bytes
     * @return the offset assigned to the published message
     */
    public long send(String topic, byte[] payload) {
        return send(topic, payload, Collections.emptyMap());
    }

    /**
     * Publishes a String message to the given topic.
     * <p>
     * The string is encoded as UTF-8 bytes before publishing.
     *
     * @param topic   the target topic name
     * @param message the string message
     * @return the offset assigned to the published message
     */
    public long send(String topic, String message) {
        return send(topic, message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Publishes a raw byte array with headers to the given topic.
     *
     * @param topic   the target topic name
     * @param payload the raw message bytes
     * @param headers optional key-value metadata
     * @return the offset assigned to the published message
     */
    public long send(String topic, byte[] payload, Map<String, String> headers) {
        long offset = client.producer().send(topic, payload, headers);
        log.debug("Message sent to topic '{}' at offset {}", topic, offset);
        return offset;
    }
}