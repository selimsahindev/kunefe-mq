package dev.selimsahin.kunefe.broker.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Append-only log for a single topic, backed by multiple log segments.
 * <p>
 * Segments are rolled when the active segment exceeds the configured size
 * or age threshold. Expired segments are deleted by RetentionManager.
 * <p>
 * Thread safety is achieved via a ReadWriteLock — writes are exclusive,
 * reads are concurrent across different segments but serialized per segment
 * due to RandomAccessFile's shared file pointer.
 */
public class TopicLog implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TopicLog.class);

    private final String topic;
    private final Path segmentDir;
    private final LogConfig config;
    private final AtomicLong nextOffset;
    private final ReadWriteLock lock;
    private final List<LogSegment> segments;

    private LogSegment activeSegment;

    public TopicLog(String topic, Path dataDir, LogConfig config) throws IOException {
        this.topic = topic;
        this.config = config;
        this.lock = new ReentrantReadWriteLock();
        this.nextOffset = new AtomicLong(0);
        this.segments = new ArrayList<>();
        this.segmentDir = dataDir.resolve(topic);

        Files.createDirectories(segmentDir);
        recoverSegments();
    }

    /**
     * Appends a message to the active segment.
     * Rolls the segment if it exceeds the configured size or age threshold.
     *
     * @return the offset assigned to the message
     */
    public long append(byte[] payload, Map<String, String> headers) throws IOException {
        lock.writeLock().lock();
        try {
            long offset = nextOffset.getAndIncrement();
            activeSegment.append(offset, payload, headers);

            if (activeSegment.shouldRoll(
                    config.getSegment().getMaxBytes(),
                    config.getSegment().getMaxHours()
            )) {
                rollSegment();
            }

            return offset;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reads all messages starting from the given offset, across all segments.
     */
    public List<LogEntry> readFrom(long fromOffset) throws IOException {
        lock.writeLock().lock();
        try {
            List<LogEntry> entries = new ArrayList<>();

            for (LogSegment segment : segments) {
                if (segment.getLastOffset() < fromOffset) {
                    continue;
                }
                entries.addAll(segment.readFrom(fromOffset));
            }

            return entries;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns all segments — used by RetentionManager for cleanup.
     */
    public List<LogSegment> getSegments() {
        lock.writeLock().lock();
        try {
            return List.copyOf(segments);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a segment from the list after RetentionManager deletes it.
     */
    public void removeSegment(LogSegment segment) {
        lock.writeLock().lock();
        try {
            segments.remove(segment);
            log.info("Segment removed from topic '{}': {}", topic, segment.getPath().getFileName());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long getNextOffset() {
        return nextOffset.get();
    }

    public String getTopic() {
        return topic;
    }

    /**
     * Rolls the active segment — deactivates it and opens a new one.
     */
    private void rollSegment() throws IOException {
        activeSegment.deactivate();
        long newBaseOffset = nextOffset.get();
        LogSegment newSegment = new LogSegment(newBaseOffset, segmentDir);
        segments.add(newSegment);
        activeSegment = newSegment;
        log.info("Rolled segment for topic '{}' — new base offset: {}", topic, newBaseOffset);
    }

    /**
     * On startup, scans the segment directory for existing segment files
     * and recovers them in order. Creates an initial segment if none exist.
     */
    private void recoverSegments() throws IOException {
        File[] segmentFiles = segmentDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".log")
        );

        if (segmentFiles == null || segmentFiles.length == 0) {
            LogSegment initial = new LogSegment(0, segmentDir);
            segments.add(initial);
            activeSegment = initial;
            log.info("No existing segments found for topic '{}' — starting fresh", topic);
            return;
        }

        Arrays.sort(segmentFiles, Comparator.comparing(File::getName));

        for (int i = 0; i < segmentFiles.length; i++) {
            long baseOffset = Long.parseLong(
                    segmentFiles[i].getName().replace(".log", "")
            );
            LogSegment segment = new LogSegment(baseOffset, segmentDir);

            if (i < segmentFiles.length - 1) {
                segment.deactivate();
            }

            segments.add(segment);
        }

        activeSegment = segments.getLast();
        nextOffset.set(activeSegment.getLastOffset() + 1);

        log.info("Recovered {} segment(s) for topic '{}' — next offset: {}",
                segments.size(), topic, nextOffset.get());
    }

    @Override
    public void close() throws IOException {
        for (LogSegment segment : segments) {
            segment.close();
        }
        log.info("TopicLog '{}' closed", topic);
    }
}