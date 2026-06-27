package dev.selimsahin.kunefe.client.consumer;

import dev.selimsahin.kunefe.proto.CommitOffsetRequest;
import dev.selimsahin.kunefe.proto.ConsumerServiceGrpc;
import dev.selimsahin.kunefe.proto.KunefeMessage;
import dev.selimsahin.kunefe.proto.RegisterConsumerGroupRequest;
import dev.selimsahin.kunefe.proto.SubscribeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Client-side consumer for receiving messages from Kunefe broker.
 * <p>
 * Wraps the gRPC ConsumerServiceBlockingStub to provide a simple
 * callback-based API for message consumption.
 */
public class KunefeConsumer {

    private static final Logger log = LoggerFactory.getLogger(KunefeConsumer.class);

    private final ConsumerServiceGrpc.ConsumerServiceBlockingStub stub;
    private final String groupId;
    private final String consumerId;

    public KunefeConsumer(
            ConsumerServiceGrpc.ConsumerServiceBlockingStub stub,
            String groupId,
            String consumerId
    ) {
        this.stub = stub;
        this.groupId = groupId;
        this.consumerId = consumerId;
    }

    /**
     * Registers this consumer group for the given topic.
     * Should be called before subscribing.
     *
     * @param topic the topic to register for
     */
    public void register(String topic) {
        RegisterConsumerGroupRequest request = RegisterConsumerGroupRequest.newBuilder()
                .setConsumerGroup(groupId)
                .setTopic(topic)
                .build();

        stub.registerConsumerGroup(request);
        log.info("Consumer group '{}' registered for topic '{}'", groupId, topic);
    }

    /**
     * Subscribes to the given topic and invokes the callback for each message.
     * <p>
     * This method blocks — it runs until the stream is closed or an error occurs.
     * Run it on a dedicated thread or virtual thread.
     *
     * @param topic           the topic to subscribe to
     * @param fromOffset      the offset to start consuming from
     * @param messageCallback called for each received message
     */
    public void subscribe(String topic, long fromOffset, Consumer<KunefeMessage> messageCallback) {
        SubscribeRequest request = SubscribeRequest.newBuilder()
                .setTopic(topic)
                .setConsumerGroup(groupId)
                .setConsumerId(consumerId)
                .setFromOffset(fromOffset)
                .build();

        log.info("Subscribing to topic '{}' from offset {}", topic, fromOffset);

        Iterator<KunefeMessage> stream = stub.subscribe(request);
        while (stream.hasNext()) {
            KunefeMessage message = stream.next();
            messageCallback.accept(message);
        }
    }

    /**
     * Commits the offset for this consumer, persisting progress to the broker.
     * <p>
     * Call after successfully processing a message to ensure at-least-once delivery.
     *
     * @param topic  the topic the message was consumed from
     * @param offset the offset of the processed message
     */
    public void commitOffset(String topic, long offset) {
        CommitOffsetRequest request = CommitOffsetRequest.newBuilder()
                .setTopic(topic)
                .setConsumerGroup(groupId)
                .setConsumerId(consumerId)
                .setOffset(offset)
                .build();

        stub.commitOffset(request);
        log.debug("Offset {} committed for topic '{}'", offset, topic);
    }
}