package dev.selimsahin.kunefe.client.producer;

import com.google.protobuf.ByteString;
import dev.selimsahin.kunefe.proto.ProducerServiceGrpc;
import dev.selimsahin.kunefe.proto.PublishRequest;
import dev.selimsahin.kunefe.proto.PublishResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Client-side producer for publishing messages to Kunefe broker.
 * <p>
 * Wraps the gRPC ProducerServiceBlockingStub to provide a clean,
 * simple API for message publishing without exposing gRPC internals.
 */
public class KunefeProducer {

    private static final Logger log = LoggerFactory.getLogger(KunefeProducer.class);

    private final ProducerServiceGrpc.ProducerServiceBlockingStub stub;

    public KunefeProducer(ProducerServiceGrpc.ProducerServiceBlockingStub stub) {
        this.stub = stub;
    }

    /**
     * Publishes a message to the given topic.
     *
     * @param topic   the target topic name
     * @param payload the raw message bytes
     * @return the offset assigned to the published message
     */
    public long send(String topic, byte[] payload) {
        return send(topic, payload, Collections.emptyMap());
    }

    /**
     * Publishes a message with headers to the given topic.
     *
     * @param topic   the target topic name
     * @param payload the raw message bytes
     * @param headers optional key-value metadata attached to the message
     * @return the offset assigned to the published message
     */
    public long send(String topic, byte[] payload, Map<String, String> headers) {
        PublishRequest request = PublishRequest.newBuilder()
                .setTopic(topic)
                .setPayload(ByteString.copyFrom(payload))
                .putAllHeaders(headers)
                .build();

        PublishResponse response = stub.publish(request);

        log.debug("Message sent to topic '{}' — offset: {}, messageId: {}",
                topic, response.getOffset(), response.getMessageId());

        return response.getOffset();
    }
}