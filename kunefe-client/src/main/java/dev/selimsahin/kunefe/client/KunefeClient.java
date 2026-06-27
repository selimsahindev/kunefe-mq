package dev.selimsahin.kunefe.client;

import dev.selimsahin.kunefe.client.consumer.KunefeConsumer;
import dev.selimsahin.kunefe.client.producer.KunefeProducer;
import dev.selimsahin.kunefe.proto.BrokerServiceGrpc;
import dev.selimsahin.kunefe.proto.ConsumerServiceGrpc;
import dev.selimsahin.kunefe.proto.CreateTopicRequest;
import dev.selimsahin.kunefe.proto.DeleteTopicRequest;
import dev.selimsahin.kunefe.proto.ListTopicsResponse;
import dev.selimsahin.kunefe.proto.ProducerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for interacting with a Kunefe broker.
 * <p>
 * Manages the gRPC channel lifecycle and provides factory methods
 * for creating producers and consumers.
 * <p>
 * Usage:
 * <pre>
 *   KunefeClient client = KunefeClient.connect("localhost", 6565);
 *   KunefeProducer producer = client.producer();
 *   producer.send("orders", payload);
 *   client.close();
 * </pre>
 */
public class KunefeClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KunefeClient.class);

    private final ManagedChannel channel;
    private final BrokerServiceGrpc.BrokerServiceBlockingStub brokerStub;
    private final ProducerServiceGrpc.ProducerServiceBlockingStub producerStub;
    private final ConsumerServiceGrpc.ConsumerServiceBlockingStub consumerStub;

    private KunefeClient(ManagedChannel channel) {
        this.channel = channel;
        this.brokerStub = BrokerServiceGrpc.newBlockingStub(channel);
        this.producerStub = ProducerServiceGrpc.newBlockingStub(channel);
        this.consumerStub = ConsumerServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Creates a new KunefeClient connected to the given broker.
     *
     * @param host broker host
     * @param port broker gRPC port
     * @return a connected KunefeClient instance
     */
    public static KunefeClient connect(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        log.info("Connected to Kunefe broker at {}:{}", host, port);
        return new KunefeClient(channel);
    }

    /**
     * Creates a new producer for publishing messages.
     */
    public KunefeProducer producer() {
        return new KunefeProducer(producerStub);
    }

    /**
     * Creates a new consumer for the given group and consumer ID.
     *
     * @param groupId    the consumer group identifier
     * @param consumerId unique identifier for this consumer instance
     */
    public KunefeConsumer consumer(String groupId, String consumerId) {
        return new KunefeConsumer(consumerStub, groupId, consumerId);
    }

    /**
     * Creates a topic on the broker.
     *
     * @param topic          the topic name
     * @param retentionHours how long messages are retained
     */
    public void createTopic(String topic, int retentionHours) {
        CreateTopicRequest request = CreateTopicRequest.newBuilder()
                .setTopic(topic)
                .setRetentionHours(retentionHours)
                .build();

        brokerStub.createTopic(request);
        log.info("Topic '{}' created", topic);
    }

    /**
     * Deletes a topic from the broker.
     *
     * @param topic the topic name to delete
     */
    public void deleteTopic(String topic) {
        DeleteTopicRequest request = DeleteTopicRequest.newBuilder()
                .setTopic(topic)
                .build();

        brokerStub.deleteTopic(request);
        log.info("Topic '{}' deleted", topic);
    }

    /**
     * Lists all topics on the broker.
     */
    public List<String> listTopics() {
        ListTopicsResponse response = brokerStub.listTopics(
                dev.selimsahin.kunefe.proto.ListTopicsRequest.newBuilder().build()
        );
        return response.getTopicsList();
    }

    /**
     * Closes the gRPC channel gracefully.
     */
    @Override
    public void close() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        log.info("KunefeClient disconnected");
    }
}