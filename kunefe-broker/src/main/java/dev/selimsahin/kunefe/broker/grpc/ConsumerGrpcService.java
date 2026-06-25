package dev.selimsahin.kunefe.broker.grpc;

import com.google.protobuf.ByteString;
import dev.selimsahin.kunefe.broker.consumer.ConsumerService;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.proto.CommitOffsetRequest;
import dev.selimsahin.kunefe.proto.CommitOffsetResponse;
import dev.selimsahin.kunefe.proto.ConsumerServiceGrpc;
import dev.selimsahin.kunefe.proto.KunefeMessage;
import dev.selimsahin.kunefe.proto.RegisterConsumerGroupRequest;
import dev.selimsahin.kunefe.proto.RegisterConsumerGroupResponse;
import dev.selimsahin.kunefe.proto.SubscribeRequest;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;

/**
 * gRPC service implementation for consumer operations.
 * <p>
 * Implements server-side streaming for the Subscribe RPC — the broker
 * pushes messages to the client over a long-lived gRPC stream rather
 * than waiting for the client to poll.
 * <p>
 * Each subscriber runs on its own virtual thread, allowing thousands of
 * concurrent consumers without exhausting platform threads.
 */
@GrpcService
public class ConsumerGrpcService extends ConsumerServiceGrpc.ConsumerServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ConsumerGrpcService.class);

    private final ConsumerService consumerService;

    public ConsumerGrpcService(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    /**
     * Opens a server-side streaming connection for a consumer.
     * <p>
     * The stream stays open until:
     * - The client cancels the subscription
     * - An unrecoverable error occurs
     * <p>
     * Each subscription runs on a dedicated virtual thread.
     * Virtual threads are cheap — blocking inside the push loop
     * does not consume a platform thread.
     */
    @Override
    public void subscribe(SubscribeRequest request,
                          StreamObserver<KunefeMessage> responseObserver) {
        ServerCallStreamObserver<KunefeMessage> streamObserver =
                (ServerCallStreamObserver<KunefeMessage>) responseObserver;

        log.info("Consumer '{}' in group '{}' subscribing to topic '{}'",
                request.getConsumerId(), request.getConsumerGroup(), request.getTopic());

        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            try {
                consumerService.subscribe(
                        request.getConsumerGroup(),
                        request.getTopic(),
                        request.getConsumerId(),
                        entry -> {
                            if (streamObserver.isCancelled()) {
                                return;
                            }

                            KunefeMessage message = KunefeMessage.newBuilder()
                                    .setMessageId(String.valueOf(entry.offset()))
                                    .setTopic(request.getTopic())
                                    .setPayload(ByteString.copyFrom(entry.payload()))
                                    .putAllHeaders(entry.headers())
                                    .setTimestamp(entry.timestamp())
                                    .build();

                            streamObserver.onNext(message);
                        },
                        () -> !streamObserver.isCancelled()
                );

                streamObserver.onCompleted();

            } catch (TopicNotFoundException e) {
                streamObserver.onError(
                        Status.NOT_FOUND
                                .withDescription(e.getMessage())
                                .asRuntimeException()
                );

            } catch (Exception e) {
                log.error("Subscription error for consumer '{}' on topic '{}'",
                        request.getConsumerId(), request.getTopic(), e);

                if (!streamObserver.isCancelled()) {
                    streamObserver.onError(
                            Status.INTERNAL
                                    .withDescription("Subscription failed")
                                    .asRuntimeException()
                    );
                }
            }
        });
    }

    /**
     * Commits the offset for a consumer, persisting progress.
     * <p>
     * Called by the client after successfully processing a message.
     * This is the at-least-once delivery guarantee in action — the broker
     * will re-deliver from the last committed offset on reconnect.
     */
    @Override
    public void commitOffset(CommitOffsetRequest request,
                             StreamObserver<CommitOffsetResponse> responseObserver) {
        try {
            consumerService.commitOffset(
                    request.getConsumerGroup(),
                    request.getTopic(),
                    request.getConsumerId(),
                    request.getOffset()
            );

            CommitOffsetResponse response = CommitOffsetResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Failed to commit offset for consumer '{}'", request.getConsumerId(), e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to commit offset")
                            .asRuntimeException()
            );
        }
    }

    /**
     * Registers a consumer group for a topic.
     */
    @Override
    public void registerConsumerGroup(RegisterConsumerGroupRequest request,
                                      StreamObserver<RegisterConsumerGroupResponse> responseObserver) {
        try {
            consumerService.registerConsumerGroup(
                    request.getConsumerGroup(),
                    request.getTopic()
            );

            RegisterConsumerGroupResponse response = RegisterConsumerGroupResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Consumer group registered: " + request.getConsumerGroup())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (TopicNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        }
    }
}