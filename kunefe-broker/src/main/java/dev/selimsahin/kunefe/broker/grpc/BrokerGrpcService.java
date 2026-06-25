package dev.selimsahin.kunefe.broker.grpc;

import dev.selimsahin.kunefe.broker.topic.Topic;
import dev.selimsahin.kunefe.broker.topic.TopicAlreadyExistsException;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.broker.topic.TopicService;
import dev.selimsahin.kunefe.proto.BrokerServiceGrpc;
import dev.selimsahin.kunefe.proto.CreateTopicRequest;
import dev.selimsahin.kunefe.proto.CreateTopicResponse;
import dev.selimsahin.kunefe.proto.DeleteTopicRequest;
import dev.selimsahin.kunefe.proto.DeleteTopicResponse;
import dev.selimsahin.kunefe.proto.ListTopicsRequest;
import dev.selimsahin.kunefe.proto.ListTopicsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * gRPC service implementation for broker management operations.
 * <p>
 * This is a thin adapter layer — it translates proto requests into domain
 * calls and proto responses back. No business logic lives here.
 * <p>
 * Error handling follows gRPC conventions: domain exceptions are caught
 * and forwarded via responseObserver.onError() rather than thrown.
 */
@GrpcService
public class BrokerGrpcService extends BrokerServiceGrpc.BrokerServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(BrokerGrpcService.class);

    private final TopicService topicService;

    public BrokerGrpcService(TopicService topicService) {
        this.topicService = topicService;
    }

    /**
     * Creates a new topic.
     * <p>
     * Returns a failure response (not an error) if the topic already exists.
     * This is intentional — topic creation is idempotent from the client's perspective.
     */
    @Override
    public void createTopic(CreateTopicRequest request,
                            StreamObserver<CreateTopicResponse> responseObserver) {
        try {
            topicService.createTopic(request.getTopic(), request.getRetentionHours());

            CreateTopicResponse response = CreateTopicResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Topic created: " + request.getTopic())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (TopicAlreadyExistsException e) {
            CreateTopicResponse response = CreateTopicResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IOException e) {
            log.error("Failed to create topic '{}'", request.getTopic(), e);
            responseObserver.onError(e);
        }
    }

    /**
     * Deletes an existing topic.
     */
    @Override
    public void deleteTopic(DeleteTopicRequest request,
                            StreamObserver<DeleteTopicResponse> responseObserver) {
        try {
            topicService.deleteTopic(request.getTopic());

            DeleteTopicResponse response = DeleteTopicResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Topic deleted: " + request.getTopic())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (TopicNotFoundException e) {
            DeleteTopicResponse response = DeleteTopicResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    /**
     * Lists all existing topics.
     */
    @Override
    public void listTopics(ListTopicsRequest request,
                           StreamObserver<ListTopicsResponse> responseObserver) {
        Collection<Topic> topics = topicService.listTopics();

        ListTopicsResponse response = ListTopicsResponse.newBuilder()
                .addAllTopics(topics.stream().map(Topic::name).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}