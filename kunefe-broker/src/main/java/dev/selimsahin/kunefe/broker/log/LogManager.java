package dev.selimsahin.kunefe.broker.log;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all topic logs.
 * <p>
 * Maintains a registry of TopicLog instances, one per topic.
 * Handles lifecycle — creation, recovery on startup, and graceful shutdown.
 * <p>
 * Uses ConcurrentHashMap for thread-safe topic registry without
 * locking the entire map on every read.
 */
@Component
public class LogManager implements LogManagerPort {

    private static final Logger log = LoggerFactory.getLogger(LogManager.class);

    private final String dataDir;
    private final LogConfig logConfig;
    private final Map<String, TopicLog> topicLogs = new ConcurrentHashMap<>();

    private Path dataDirPath;

    public LogManager(
            @Value("${kunefe.data.dir:./kunefe-data}") String dataDir,
            LogConfig logConfig
    ) {
        this.dataDir = dataDir;
        this.logConfig = logConfig;
    }

    /**
     * On startup, ensures the data directory exists and recovers
     * any existing topic logs from disk.
     */
    @PostConstruct
    public void init() throws IOException {
        dataDirPath = Paths.get(dataDir);
        Files.createDirectories(dataDirPath);
        log.info("LogManager initialized — data dir: {}", dataDirPath.toAbsolutePath());
        recoverExistingLogs();
    }

    /**
     * Appends a message to the given topic's log.
     * Creates the topic log if it does not exist yet.
     *
     * @return the offset assigned to the message
     */
    @Override
    public long append(String topic, byte[] payload, Map<String, String> headers) throws IOException {
        TopicLog topicLog = getOrCreateLog(topic);
        return topicLog.append(payload, headers);
    }

    /**
     * Reads all messages from the given topic starting at fromOffset.
     */
    @Override
    public List<LogEntry> readFrom(String topic, long fromOffset) throws IOException {
        TopicLog topicLog = getOrCreateLog(topic);
        return topicLog.readFrom(fromOffset);
    }

    /**
     * Returns the next offset for the given topic.
     */
    @Override
    public long getNextOffset(String topic) throws IOException {
        TopicLog topicLog = getOrCreateLog(topic);
        return topicLog.getNextOffset();
    }

    /**
     * Checks whether a topic log exists.
     */
    @Override
    public boolean topicExists(String topic) {
        return topicLogs.containsKey(topic) ||
                dataDirPath.resolve(topic).toFile().isDirectory();
    }

    /**
     * Creates a new topic log explicitly.
     * Called by TopicService when a topic is created via gRPC.
     */
    @Override
    public void createTopic(String topic) throws IOException {
        if (topicExists(topic)) {
            log.warn("Topic '{}' already exists, skipping creation", topic);
            return;
        }
        getOrCreateLog(topic);
        log.info("Topic '{}' created", topic);
    }

    /**
     * Returns all currently known topic names.
     */
    @Override
    public List<String> listTopics() {
        return List.copyOf(topicLogs.keySet());
    }

    /**
     * Returns the TopicLog for the given topic — used by RetentionManager.
     */
    public TopicLog getTopicLog(String topic) {
        return topicLogs.get(topic);
    }

    /**
     * Gets the TopicLog for the given topic, creating it if necessary.
     * computeIfAbsent is atomic — only one TopicLog is ever created per topic.
     */
    private TopicLog getOrCreateLog(String topic) throws IOException {
        TopicLog topicLog = topicLogs.get(topic);
        if (topicLog != null) {
            return topicLog;
        }

        try {
            return topicLogs.computeIfAbsent(topic, t -> {
                try {
                    return new TopicLog(t, dataDirPath, logConfig);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create TopicLog for topic: " + t, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    /**
     * On startup, scans the data directory for existing topic directories
     * and recovers them into memory.
     */
    private void recoverExistingLogs() throws IOException {
        File[] topicDirs = dataDirPath.toFile().listFiles(File::isDirectory);

        if (topicDirs == null || topicDirs.length == 0) {
            log.info("No existing topic logs found — starting fresh");
            return;
        }

        for (File topicDir : topicDirs) {
            String topic = topicDir.getName();
            TopicLog topicLog = new TopicLog(topic, dataDirPath, logConfig);
            topicLogs.put(topic, topicLog);
            log.info("Recovered topic log: '{}'", topic);
        }
    }

    /**
     * Gracefully closes all topic logs on shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down LogManager — flushing {} topic logs", topicLogs.size());
        topicLogs.values().forEach(topicLog -> {
            try {
                topicLog.close();
            } catch (IOException e) {
                log.error("Failed to close topic log: {}", topicLog.getTopic(), e);
            }
        });
    }
}