package dev.selimsahin.kunefe.broker.consumer;

import dev.selimsahin.kunefe.broker.log.LogEntry;
import dev.selimsahin.kunefe.broker.log.LogManager;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.broker.topic.TopicService;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages consumer group lifecycle and message delivery.
 * <p>
 * Implements a push-based consumption model — when a consumer subscribes,
 * it registers a callback. The broker calls this callback whenever new
 * messages are available, rather than waiting for the consumer to poll.
 * <p>
 * This is the key architectural difference from Kafka, which is pull-based.
 */
@Service
public class ConsumerService {

    private static final Logger log = LoggerFactory.getLogger(ConsumerService.class);

    private static final long PUSH_INTERVAL_MS = 100;

    private final ConsumerGroupRegistry groupRegistry;
    private final OffsetStore offsetStore;
    private final LogManager logManager;
    private final TopicService topicService;

    public ConsumerService(
            ConsumerGroupRegistry groupRegistry,
            OffsetStore offsetStore,
            LogManager logManager,
            TopicService topicService
    ) {
        this.groupRegistry = groupRegistry;
        this.offsetStore = offsetStore;
        this.logManager = logManager;
        this.topicService = topicService;
    }

    /**
     * Registers a consumer group and topic pair.
     * Creates the group if it does not exist yet.
     */
    public void registerConsumerGroup(String groupId, String topic) {
        if (!topicService.topicExists(topic)) {
            throw new TopicNotFoundException(topic);
        }
        groupRegistry.register(groupId, topic);
        log.info("Consumer group '{}' registered for topic '{}'", groupId, topic);
    }

    /**
     * Subscribes a consumer to a topic within a group.
     * <p>
     * Starts pushing messages from the last committed offset.
     * The messageCallback is invoked for each message delivered to this consumer.
     * <p>
     * This method blocks — it runs the push loop on the calling thread.
     * In the gRPC layer, each subscriber gets its own virtual thread.
     *
     * @param groupId         the consumer group identifier
     * @param topic           the topic to subscribe to
     * @param consumerId      unique identifier for this consumer instance
     * @param messageCallback called for each message pushed to this consumer
     * @param isActive        supplier checked each iteration — false stops the loop
     */
    public void subscribe(
            String groupId,
            String topic,
            String consumerId,
            Consumer<LogEntry> messageCallback,
            java.util.function.BooleanSupplier isActive
    ) throws IOException, RocksDBException, InterruptedException {
        if (!topicService.topicExists(topic)) {
            throw new TopicNotFoundException(topic);
        }

        ConsumerGroup group = groupRegistry.register(groupId, topic);
        group.addConsumer(consumerId);

        long fromOffset = offsetStore.get(groupId, topic, consumerId);
        log.info("Consumer '{}' in group '{}' subscribing to '{}' from offset {}",
                consumerId, groupId, topic, fromOffset);

        try {
            pushLoop(groupId, topic, consumerId, fromOffset, messageCallback, isActive);
        } finally {
            group.removeConsumer(consumerId);
            log.info("Consumer '{}' unsubscribed from topic '{}'", consumerId, topic);
        }
    }

    /**
     * Commits the offset for a consumer, persisting progress to RocksDB.
     * <p>
     * The committed offset is offset + 1, meaning "I have processed this message,
     * start me from the next one on restart."
     */
    public void commitOffset(String groupId, String topic, String consumerId, long offset)
            throws RocksDBException {
        offsetStore.commit(groupId, topic, consumerId, offset + 1);
        log.debug("Offset committed — group: '{}', topic: '{}', offset: {}", groupId, topic, offset);
    }

    /**
     * Push loop — continuously checks for new messages and delivers them.
     * <p>
     * Sleeps between iterations to avoid busy-waiting.
     * Virtual threads make this sleep cheap — no platform thread is blocked.
     */
    private void pushLoop(
            String groupId,
            String topic,
            String consumerId,
            long fromOffset,
            Consumer<LogEntry> messageCallback,
            java.util.function.BooleanSupplier isActive
    ) throws IOException, InterruptedException {
        long currentOffset = fromOffset;

        while (isActive.getAsBoolean()) {
            List<LogEntry> entries = logManager.readFrom(topic, currentOffset);

            if (!entries.isEmpty()) {
                log.debug("Pushing {} messages to consumer '{}' in group '{}' on topic '{}'",
                        entries.size(), consumerId, groupId, topic);
            }

            for (LogEntry entry : entries) {
                if (!isActive.getAsBoolean()) {
                    return;
                }
                messageCallback.accept(entry);
                currentOffset = entry.offset() + 1;
            }

            if (entries.isEmpty()) {
                Thread.sleep(PUSH_INTERVAL_MS);
            }
        }
    }
}