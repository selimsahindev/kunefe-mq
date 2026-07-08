package dev.selimsahin.kunefe.broker.log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Port interface for log management operations.
 * <p>
 * Abstracts the append-only log engine behind an interface,
 * enabling dependency inversion and easier testing.
 */
public interface LogManagerPort {

    long append(String topic, byte[] payload, Map<String, String> headers) throws IOException;

    List<LogEntry> readFrom(String topic, long fromOffset) throws IOException;

    long getNextOffset(String topic) throws IOException;

    boolean topicExists(String topic);

    void createTopic(String topic) throws IOException;

    List<String> listTopics();
}