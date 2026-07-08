package dev.selimsahin.kunefe.broker.producer;

import dev.selimsahin.kunefe.broker.log.LogManagerPort;
import dev.selimsahin.kunefe.broker.topic.TopicNotFoundException;
import dev.selimsahin.kunefe.broker.topic.TopicServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProducerService message publishing logic.
 * <p>
 * Uses Mockito to isolate ProducerService from LogManager and TopicService.
 */
@ExtendWith(MockitoExtension.class)
class ProducerServiceTest {

    @Mock
    private LogManagerPort logManager;

    @Mock
    private TopicServicePort topicService;

    @InjectMocks
    private ProducerService producerService;

    @Test
    void givenExistingTopic_whenMessagePublished_thenOffsetReturned() throws IOException {
        when(topicService.topicExists("orders")).thenReturn(true);
        when(logManager.append("orders", "hello".getBytes(), Map.of())).thenReturn(0L);

        long offset = producerService.publish("orders", "hello".getBytes(), Map.of());

        assertEquals(0L, offset);
    }

    @Test
    void givenExistingTopic_whenMessagePublished_thenMessageCountIncremented() throws IOException {
        when(topicService.topicExists("orders")).thenReturn(true);
        when(logManager.append(eq("orders"), any(), any())).thenReturn(0L);

        producerService.publish("orders", "hello".getBytes(), Map.of());

        verify(topicService).incrementMessageCount("orders");
    }

    @Test
    void givenNonExistentTopic_whenMessagePublished_thenExceptionThrown() {
        when(topicService.topicExists("orders")).thenReturn(false);

        assertThrows(TopicNotFoundException.class, () -> {
            try {
                producerService.publish("orders", "hello".getBytes(), Map.of());
            } catch (IOException e) {
                fail("Unexpected IOException: " + e.getMessage());
            }
        });

        verifyNoInteractions(logManager);
    }

    @Test
    void givenExistingTopic_whenMessagePublishedWithHeaders_thenHeadersPassedToLog() throws IOException {
        Map<String, String> headers = Map.of("source", "test");
        when(topicService.topicExists("orders")).thenReturn(true);
        when(logManager.append("orders", "hello".getBytes(), headers)).thenReturn(0L);

        producerService.publish("orders", "hello".getBytes(), headers);

        verify(logManager).append("orders", "hello".getBytes(), headers);
    }
}