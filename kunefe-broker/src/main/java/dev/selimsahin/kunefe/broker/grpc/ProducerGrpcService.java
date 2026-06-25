package dev.selimsahin.kunefe.broker.grpc;

import dev.selimsahin.kunefe.broker.producer.ProducerService;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.proto.ProducerServiceGrpc;
import dev.selimsahin.kunefe.proto.PublishRequest;
import dev.selimsahin.kunefe.proto.PublishResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * gRPC service implementation for message publishing.
 * <p>
 * Thin adapter between the gRPC layer and ProducerService.
 * Translates proto requests to domain calls and handles errors
 * using gRPC status codes.
 */
@GrpcService
public class ProducerGrpcService extends ProducerServiceGrpc.ProducerServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ProducerGrpcService.class);

    private final ProducerService producerService;

    public ProducerGrpcService(ProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * Publishes a message to the given topic.
     * <p>
     * Generates a unique message ID on the broker side — clients do not
     * need to supply IDs, which simplifies the producer API.
     */
    @Override
    public void publish(PublishRequest request,
                        StreamObserver<PublishResponse> responseObserver) {
        try {
            long offset = producerService.publish(
                    request.getTopic(),
                    request.getPayload().toByteArray(),
                    request.getHeadersMap()
            );

            String messageId = UUID.randomUUID().toString();

            PublishResponse response = PublishResponse.newBuilder()
                    .setSuccess(true)
                    .setMessageId(messageId)
                    .setOffset(offset)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (TopicNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );

        } catch (IOException e) {
            log.error("Failed to publish message to topic '{}'", request.getTopic(), e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to publish message")
                            .asRuntimeException()
            );
        }
    }
}