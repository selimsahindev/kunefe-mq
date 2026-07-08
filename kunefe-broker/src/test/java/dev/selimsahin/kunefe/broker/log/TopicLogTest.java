package dev.selimsahin.kunefe.broker.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TopicLog append-only log operations.
 * <p>
 * Uses @TempDir to create a temporary directory for each test,
 * ensuring test isolation and automatic cleanup after each test.
 */
class TopicLogTest {

    @TempDir
    Path tempDir;

    private TopicLog topicLog;

    @BeforeEach
    void setUp() throws IOException {
        topicLog = new TopicLog("test-topic", tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        topicLog.close();
    }

    @Test
    void givenEmptyLog_whenMessageAppended_thenOffsetStartsFromZero() throws IOException {
        byte[] payload = "hello world".getBytes();

        long offset = topicLog.append(payload, Map.of());

        assertEquals(0L, offset);
    }

    @Test
    void givenEmptyLog_whenMultipleMessagesAppended_thenOffsetsIncrementSequentially() throws IOException {
        byte[] payload = "message".getBytes();

        long firstOffset = topicLog.append(payload, Map.of());
        long secondOffset = topicLog.append(payload, Map.of());
        long thirdOffset = topicLog.append(payload, Map.of());

        assertEquals(0L, firstOffset);
        assertEquals(1L, secondOffset);
        assertEquals(2L, thirdOffset);
    }

    @Test
    void givenAppendedMessage_whenReadFromZero_thenMessageIsReturned() throws IOException {
        byte[] payload = "hello world".getBytes();

        topicLog.append(payload, Map.of());
        List<LogEntry> entries = topicLog.readFrom(0);

        assertEquals(1, entries.size());
        assertArrayEquals(payload, entries.getFirst().payload());
    }

    @Test
    void givenMultipleMessages_whenReadFromZero_thenAllMessagesReturned() throws IOException {
        topicLog.append("message-0".getBytes(), Map.of());
        topicLog.append("message-1".getBytes(), Map.of());
        topicLog.append("message-2".getBytes(), Map.of());

        List<LogEntry> entries = topicLog.readFrom(0);

        assertEquals(3, entries.size());
        assertArrayEquals("message-0".getBytes(), entries.get(0).payload());
        assertArrayEquals("message-1".getBytes(), entries.get(1).payload());
        assertArrayEquals("message-2".getBytes(), entries.get(2).payload());
    }

    @Test
    void givenMultipleMessages_whenReadFromMiddleOffset_thenOnlyRemainingMessagesReturned() throws IOException {
        topicLog.append("message-0".getBytes(), Map.of());
        topicLog.append("message-1".getBytes(), Map.of());
        topicLog.append("message-2".getBytes(), Map.of());

        List<LogEntry> entries = topicLog.readFrom(1);

        assertEquals(2, entries.size());
        assertArrayEquals("message-1".getBytes(), entries.get(0).payload());
        assertArrayEquals("message-2".getBytes(), entries.get(1).payload());
    }

    @Test
    void givenEmptyLog_whenReadFromZero_thenEmptyListReturned() throws IOException {
        List<LogEntry> entries = topicLog.readFrom(0);

        assertTrue(entries.isEmpty());
    }

    @Test
    void givenAppendedMessage_whenLogRestartedAndReadFromZero_thenMessagePersisted() throws IOException {
        byte[] payload = "persistent message".getBytes();
        topicLog.append(payload, Map.of());
        topicLog.close();

        // Simulate broker restart, create new TopicLog instance
        TopicLog recoveredLog = new TopicLog("test-topic", tempDir);
        List<LogEntry> entries = recoveredLog.readFrom(0);
        recoveredLog.close();

        assertEquals(1, entries.size());
        assertArrayEquals(payload, entries.getFirst().payload());
    }

    @Test
    void givenAppendedMessages_whenLogRestarted_thenNextOffsetRecovered() throws IOException {
        topicLog.append("message-0".getBytes(), Map.of());
        topicLog.append("message-1".getBytes(), Map.of());
        topicLog.close();

        // Simulate broker restart
        TopicLog recoveredLog = new TopicLog("test-topic", tempDir);
        long nextOffset = recoveredLog.getNextOffset();
        recoveredLog.close();

        assertEquals(2L, nextOffset);
    }

    @Test
    void givenMessageWithHeaders_whenRead_thenHeadersPreserved() throws IOException {
        byte[] payload = "message".getBytes();
        Map<String, String> headers = Map.of("source", "test", "version", "1");

        topicLog.append(payload, headers);
        List<LogEntry> entries = topicLog.readFrom(0);

        assertEquals(1, entries.size());
        assertEquals("test", entries.getFirst().headers().get("source"));
        assertEquals("1", entries.getFirst().headers().get("version"));
    }

    @Test
    void givenEmptyPayload_whenAppended_thenMessageStoredSuccessfully() throws IOException {
        byte[] emptyPayload = new byte[0];

        long offset = topicLog.append(emptyPayload, Map.of());
        List<LogEntry> entries = topicLog.readFrom(0);

        assertEquals(0L, offset);
        assertEquals(1, entries.size());
        assertArrayEquals(emptyPayload, entries.getFirst().payload());
    }

    @Test
    void givenAppendedMessage_whenRead_thenTimestampWithinExpectedRange() throws IOException {
        long before = System.currentTimeMillis();
        topicLog.append("message".getBytes(), Map.of());
        long after = System.currentTimeMillis();

        List<LogEntry> entries = topicLog.readFrom(0);

        assertTrue(entries.getFirst().timestamp() >= before);
        assertTrue(entries.getFirst().timestamp() <= after);
    }
}