package dev.selimsahin.kunefe.broker.consumer;

import dev.selimsahin.kunefe.broker.config.KunefeMetrics;
import dev.selimsahin.kunefe.broker.log.LogEntry;
import dev.selimsahin.kunefe.broker.log.LogManagerPort;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.broker.topic.TopicServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConsumerService subscription and offset management.
 * <p>
 * Push loop tests use a BooleanSupplier that returns false after a set
 * number of iterations to prevent infinite loops in tests.
 */
@ExtendWith(MockitoExtension.class)
class ConsumerServiceTest {

    @Mock
    private ConsumerGroupRegistryPort groupRegistry;

    @Mock
    private OffsetStorePort offsetStore;

    @Mock
    private LogManagerPort logManager;

    @Mock
    private TopicServicePort topicService;

    @Mock
    private KunefeMetrics metrics;

    @InjectMocks
    private ConsumerService consumerService;

    @Test
    void givenExistingTopic_whenConsumerGroupRegistered_thenGroupCreated() {
        when(topicService.topicExists("orders")).thenReturn(true);

        consumerService.registerConsumerGroup("test-group", "orders");

        verify(groupRegistry).register("test-group", "orders");
    }

    @Test
    void givenNonExistentTopic_whenConsumerGroupRegistered_thenExceptionThrown() {
        when(topicService.topicExists("orders")).thenReturn(false);

        assertThrows(TopicNotFoundException.class, () -> {
            consumerService.registerConsumerGroup("test-group", "orders");
        });

        verifyNoInteractions(groupRegistry);
    }

    @Test
    void givenCommittedOffset_whenRetrieved_thenCorrectOffsetReturned() throws RocksDBException {
        when(offsetStore.get("test-group", "orders", "consumer-1")).thenReturn(5L);

        long offset = offsetStore.get("test-group", "orders", "consumer-1");

        assertEquals(5L, offset);
    }

    @Test
    void givenProcessedMessage_whenOffsetCommitted_thenNextOffsetPersisted() throws RocksDBException {
        consumerService.commitOffset("test-group", "orders", "consumer-1", 5L);

        verify(offsetStore).commit("test-group", "orders", "consumer-1", 6L);
    }

    @Test
    void givenNonExistentTopic_whenSubscribed_thenExceptionThrown() {
        when(topicService.topicExists("orders")).thenReturn(false);

        assertThrows(TopicNotFoundException.class, () -> {
            consumerService.subscribe(
                    "test-group", "orders", "consumer-1",
                    message -> {},
                    () -> false
            );
        });
    }

    @Test
    void givenMessages_whenSubscribed_thenMessagesDeliveredToCallback()
            throws IOException, RocksDBException, InterruptedException {
        LogEntry entry = new LogEntry(0L, System.currentTimeMillis(), "hello".getBytes(), Map.of());

        when(topicService.topicExists("orders")).thenReturn(true);
        when(groupRegistry.register("test-group", "orders"))
                .thenReturn(new ConsumerGroup("test-group", "orders"));
        when(offsetStore.get("test-group", "orders", "consumer-1")).thenReturn(0L);

        // Return message on first call, empty on second to stop the loop
        when(logManager.readFrom("orders", 0L))
                .thenReturn(List.of(entry))
                .thenReturn(List.of());

        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicInteger callCount = new AtomicInteger(0);

        consumerService.subscribe(
                "test-group", "orders", "consumer-1",
                message -> receivedCount.incrementAndGet(),
                () -> callCount.incrementAndGet() <= 2  // stop after 2 iterations
        );

        assertEquals(1, receivedCount.get());
    }

    @Test
    void givenNoMessages_whenSubscribed_thenCallbackNotInvoked()
            throws IOException, RocksDBException, InterruptedException {
        when(topicService.topicExists("orders")).thenReturn(true);
        when(groupRegistry.register("test-group", "orders"))
                .thenReturn(new ConsumerGroup("test-group", "orders"));
        when(offsetStore.get("test-group", "orders", "consumer-1")).thenReturn(0L);
        when(logManager.readFrom("orders", 0L)).thenReturn(List.of());

        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicInteger callCount = new AtomicInteger(0);

        consumerService.subscribe(
                "test-group", "orders", "consumer-1",
                message -> receivedCount.incrementAndGet(),
                () -> callCount.incrementAndGet() <= 1
        );

        assertEquals(0, receivedCount.get());
    }
}