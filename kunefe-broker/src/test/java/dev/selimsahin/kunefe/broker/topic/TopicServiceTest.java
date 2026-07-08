package dev.selimsahin.kunefe.broker.topic;

import dev.selimsahin.kunefe.broker.log.LogManagerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TopicService business logic.
 * <p>
 * Uses Mockito to isolate TopicService from its dependencies.
 * LogManager and TopicRegistry are mocked to avoid disk I/O in tests.
 */
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    private LogManagerPort logManager;

    @Mock
    private TopicRegistryPort topicRegistry;

    @InjectMocks
    private TopicService topicService;

    @Test
    void givenValidTopic_whenCreated_thenTopicIsRegistered() throws IOException {
        when(topicRegistry.exists("orders")).thenReturn(false);

        Topic topic = topicService.createTopic("orders", 24);

        assertEquals("orders", topic.name());
        assertEquals(24, topic.retentionHours());
        verify(topicRegistry).register(any(Topic.class));
        verify(logManager).createTopic("orders");
    }

    @Test
    void givenExistingTopic_whenCreatedAgain_thenExceptionThrown() throws IOException  {
        when(topicRegistry.exists("orders")).thenReturn(true);

        assertThrows(TopicAlreadyExistsException.class, () -> {
            topicService.createTopic("orders", 24);
        });

        verify(topicRegistry, never()).register(any());
        verify(logManager, never()).createTopic(any());
    }

    @Test
    void givenExistingTopic_whenDeleted_thenTopicIsRemoved() {
        when(topicRegistry.exists("orders")).thenReturn(true);

        topicService.deleteTopic("orders");

        verify(topicRegistry).remove("orders");
    }

    @Test
    void givenNonExistentTopic_whenDeleted_thenExceptionThrown() {
        when(topicRegistry.exists("orders")).thenReturn(false);

        assertThrows(TopicNotFoundException.class, () -> {
            topicService.deleteTopic("orders");
        });

        verify(topicRegistry, never()).remove(any());
    }

    @Test
    void givenExistingTopic_whenQueried_thenTopicReturned() {
        Topic expected = Topic.withDefaults("orders");
        when(topicRegistry.find("orders")).thenReturn(java.util.Optional.of(expected));

        Topic result = topicService.getTopic("orders");

        assertEquals("orders", result.name());
    }

    @Test
    void givenNonExistentTopic_whenQueried_thenExceptionThrown() {
        when(topicRegistry.find("orders")).thenReturn(java.util.Optional.empty());

        assertThrows(TopicNotFoundException.class, () -> {
            topicService.getTopic("orders");
        });
    }

    @Test
    void givenExistingTopic_whenMessageCountIncremented_thenCountUpdated() {
        Topic topic = Topic.withDefaults("orders");
        when(topicRegistry.find("orders")).thenReturn(java.util.Optional.of(topic));

        topicService.incrementMessageCount("orders");

        verify(topicRegistry).update(argThat(t -> t.messageCount() == 1));
    }

    @Test
    void givenMultipleTopics_whenListed_thenAllTopicsReturned() {
        Collection<Topic> topics = java.util.List.of(
                Topic.withDefaults("orders"),
                Topic.withDefaults("payments")
        );
        when(topicRegistry.findAll()).thenReturn(topics);

        Collection<Topic> result = topicService.listTopics();

        assertEquals(2, result.size());
    }
}