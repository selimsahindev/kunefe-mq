package dev.selimsahin.kunefe.test;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests for Kunefe broker.
 * <p>
 * Tests the full message lifecycle — topic creation, publishing,
 * subscribing, and offset committing — against a real broker instance.
 */
class BrokerIntegrationTest extends BaseIntegrationTest {

    @Test
    void givenBroker_whenTopicCreated_thenTopicAppearsInList() {
        client.createTopic("integration-test-topic", 24);

        List<String> topics = client.listTopics();

        assertTrue(topics.contains("integration-test-topic"));
    }

    @Test
    void givenExistingTopic_whenMessagePublished_thenOffsetReturned() {
        client.createTopic("publish-test-topic", 24);

        long offset = client.producer().send("publish-test-topic", "hello".getBytes());

        assertEquals(0L, offset);
    }

    @Test
    void givenPublishedMessage_whenSubscribed_thenMessageReceived()
            throws InterruptedException {
        String topic = "subscribe-test-topic";
        client.createTopic(topic, 24);
        client.producer().send(topic, "hello world".getBytes());

        // CountDownLatch: blocks the test thread until a message is received
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<byte[]> receivedPayload = new AtomicReference<>();

        Thread.ofVirtual().start(() -> {
            client.consumer("test-group", "consumer-1").subscribe(
                    topic, 0L,
                    message -> {
                        receivedPayload.set(message.getPayload().toByteArray());
                        latch.countDown();
                    }
            );
        });

        // Waits up to 5 seconds. Test fails if no message is received within timeout
        boolean received = latch.await(5, TimeUnit.SECONDS);

        assertTrue(received, "Message was not received within timeout");
        assertArrayEquals("hello world".getBytes(), receivedPayload.get());
    }

    @Test
    void givenMultipleMessages_whenSubscribed_thenAllMessagesReceived()
            throws InterruptedException {
        String topic = "multi-message-topic";
        client.createTopic(topic, 24);

        client.producer().send(topic, "message-0".getBytes());
        client.producer().send(topic, "message-1".getBytes());
        client.producer().send(topic, "message-2".getBytes());

        CountDownLatch latch = new CountDownLatch(3);

        Thread.ofVirtual().start(() -> {
            client.consumer("test-group", "consumer-1").subscribe(
                    topic, 0L,
                    message -> latch.countDown()
            );
        });

        boolean received = latch.await(5, TimeUnit.SECONDS);

        assertTrue(received, "Not all messages were received within timeout");
    }
}