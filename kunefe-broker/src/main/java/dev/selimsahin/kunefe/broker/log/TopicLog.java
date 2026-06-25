package dev.selimsahin.kunefe.broker.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Append-only log for a single topic backed by a RandomAccessFile.
 * <p>
 * Each message is written sequentially to disk. The file pointer is
 * tracked explicitly to support both appending and reading from any offset.
 * <p>
 * Thread safety is achieved via a ReadWriteLock:
 * - Multiple concurrent readers are allowed
 * - Writes are exclusive
 */
public class TopicLog implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TopicLog.class);

    private final String topic;
    private final RandomAccessFile file;
    private final AtomicLong nextOffset;
    private final ReadWriteLock lock;
    private long writePosition;

    public TopicLog(String topic, Path dataDir) throws IOException {
        this.topic = topic;
        this.lock = new ReentrantReadWriteLock();
        this.nextOffset = new AtomicLong(0);

        Path logFile = dataDir.resolve(topic + ".log");
        this.file = new RandomAccessFile(logFile.toFile(), "rw");

        recoverOffset();
    }

    /**
     * Appends a message to the log and returns its assigned offset.
     * <p>
     * Write format per entry:
     * [offset: 8B][timestamp: 8B][headersLen: 4B][payloadLen: 4B][payload][headers]
     */
    public long append(byte[] payload, Map<String, String> headers) throws IOException {
        lock.writeLock().lock();
        try {
            long offset = nextOffset.getAndIncrement();
            long timestamp = System.currentTimeMillis();
            byte[] headersBytes = serializeHeaders(headers);

            ByteBuffer buffer = ByteBuffer.allocate(
                    LogEntry.FIXED_HEADER_SIZE + payload.length + headersBytes.length
            );

            buffer.putLong(offset);
            buffer.putLong(timestamp);
            buffer.putInt(headersBytes.length);
            buffer.putInt(payload.length);
            buffer.put(payload);
            buffer.put(headersBytes);

            file.seek(writePosition);
            file.write(buffer.array());
            writePosition += buffer.capacity();

            log.debug("Appended message to topic '{}' at offset {}", topic, offset);
            return offset;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reads all messages starting from the given offset.
     */
    public List<LogEntry> readFrom(long fromOffset) throws IOException {
        lock.readLock().lock();
        try {
            List<LogEntry> entries = new ArrayList<>();
            long readPosition = 0;

            while (readPosition + LogEntry.FIXED_HEADER_SIZE <= writePosition) {
                file.seek(readPosition);

                long offset = file.readLong();
                long timestamp = file.readLong();
                int headersLen = file.readInt();
                int payloadLen = file.readInt();

                byte[] payload = new byte[payloadLen];
                file.readFully(payload);

                byte[] headersBytes = new byte[headersLen];
                file.readFully(headersBytes);

                Map<String, String> entryHeaders = deserializeHeaders(headersBytes);

                if (offset >= fromOffset) {
                    entries.add(new LogEntry(offset, timestamp, payload, entryHeaders));
                }

                readPosition += LogEntry.FIXED_HEADER_SIZE + payloadLen + headersLen;
            }

            return entries;
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getNextOffset() {
        return nextOffset.get();
    }

    public String getTopic() {
        return topic;
    }

    /**
     * Scans the existing log file on startup to recover the next offset.
     * Ensures offset continuity across broker restarts.
     */
    private void recoverOffset() throws IOException {
        long fileLength = file.length();
        long readPosition = 0;
        long lastOffset = -1;

        while (readPosition + LogEntry.FIXED_HEADER_SIZE <= fileLength) {
            file.seek(readPosition);

            long offset = file.readLong();
            long timestamp = file.readLong();
            int headersLen = file.readInt();
            int payloadLen = file.readInt();

            if (timestamp == 0) {
                break;
            }

            lastOffset = offset;
            readPosition += LogEntry.FIXED_HEADER_SIZE + payloadLen + headersLen;
        }

        writePosition = readPosition;
        nextOffset.set(lastOffset + 1);
        log.info("Recovered topic '{}' — next offset: {}", topic, nextOffset.get());
    }

    private byte[] serializeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return new byte[0];
        }

        int totalSize = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            totalSize += 4 + entry.getKey().getBytes(StandardCharsets.UTF_8).length;
            totalSize += 4 + entry.getValue().getBytes(StandardCharsets.UTF_8).length;
        }

        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] valueBytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            buf.putInt(keyBytes.length);
            buf.put(keyBytes);
            buf.putInt(valueBytes.length);
            buf.put(valueBytes);
        }

        return buf.array();
    }

    private Map<String, String> deserializeHeaders(byte[] bytes) {
        Map<String, String> headers = new HashMap<>();
        if (bytes.length == 0) {
            return headers;
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        while (buf.remaining() > 0) {
            int keyLen = buf.getInt();
            byte[] keyBytes = new byte[keyLen];
            buf.get(keyBytes);

            int valueLen = buf.getInt();
            byte[] valueBytes = new byte[valueLen];
            buf.get(valueBytes);

            headers.put(
                    new String(keyBytes, StandardCharsets.UTF_8),
                    new String(valueBytes, StandardCharsets.UTF_8)
            );
        }

        return headers;
    }

    @Override
    public void close() throws IOException {
        file.close();
        log.info("TopicLog '{}' closed", topic);
    }
}