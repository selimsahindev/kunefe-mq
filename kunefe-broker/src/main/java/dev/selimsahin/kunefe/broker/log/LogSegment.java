package dev.selimsahin.kunefe.broker.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single immutable log segment for a topic.
 * <p>
 * Each segment is backed by a single file on disk. Once a segment is rolled
 * (closed), it is never written to again — only read or deleted.
 * <p>
 * File naming convention: {baseOffset}.log
 * The base offset is the offset of the first message in this segment.
 * This allows efficient lookup of which segment contains a given offset.
 */
public class LogSegment implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LogSegment.class);

    private final long baseOffset;
    private final Path path;
    private final Instant createdAt;
    private final RandomAccessFile file;

    private long writePosition;
    private long lastOffset;
    private boolean active;

    public LogSegment(long baseOffset, Path segmentDir) throws IOException {
        this.baseOffset = baseOffset;
        this.createdAt = Instant.now();
        this.active = true;
        this.lastOffset = baseOffset - 1;

        String fileName = String.format("%020d.log", baseOffset);
        this.path = segmentDir.resolve(fileName);
        this.file = new RandomAccessFile(path.toFile(), "rw");
        this.writePosition = file.length();

        if (writePosition > 0) {
            recoverLastOffset();
        }
    }

    /**
     * Appends a message to this segment.
     * <p>
     * Write format per entry:
     * [offset: 8B][timestamp: 8B][headersLen: 4B][payloadLen: 4B][payload][headers]
     *
     * @return the offset assigned to the message
     */
    public long append(long offset, byte[] payload, Map<String, String> headers) throws IOException {
        byte[] headersBytes = serializeHeaders(headers);

        ByteBuffer buffer = ByteBuffer.allocate(
                LogEntry.FIXED_HEADER_SIZE + payload.length + headersBytes.length
        );

        buffer.putLong(offset);
        buffer.putLong(System.currentTimeMillis());
        buffer.putInt(headersBytes.length);
        buffer.putInt(payload.length);
        buffer.put(payload);
        buffer.put(headersBytes);

        file.seek(writePosition);
        file.write(buffer.array());
        writePosition += buffer.capacity();
        lastOffset = offset;

        log.debug("Appended message to segment '{}' at offset {}", path.getFileName(), offset);
        return offset;
    }

    /**
     * Reads all messages in this segment starting from the given offset.
     */
    public List<LogEntry> readFrom(long fromOffset) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        long readPosition = 0;

        while (readPosition + LogEntry.FIXED_HEADER_SIZE <= writePosition) {
            file.seek(readPosition);

            if (readPosition + LogEntry.FIXED_HEADER_SIZE > writePosition) {
                break;
            }

            long offset = file.readLong();
            long timestamp = file.readLong();
            int headersLen = file.readInt();
            int payloadLen = file.readInt();

            long nextPosition = readPosition + LogEntry.FIXED_HEADER_SIZE + payloadLen + headersLen;
            if (nextPosition > writePosition) {
                break;
            }

            byte[] payload = new byte[payloadLen];
            file.readFully(payload);

            byte[] headersBytes = new byte[headersLen];
            file.readFully(headersBytes);

            if (offset >= fromOffset) {
                entries.add(new LogEntry(offset, timestamp, payload, deserializeHeaders(headersBytes)));
            }

            readPosition = nextPosition;
        }

        return entries;
    }

    /**
     * Returns true if this segment should be rolled based on size or age.
     */
    public boolean shouldRoll(long maxBytes, int maxHours) {
        boolean tooLarge = writePosition >= maxBytes;
        boolean tooOld = createdAt.isBefore(
                Instant.now().minusSeconds(maxHours * 3600L)
        );
        return tooLarge || tooOld;
    }

    /**
     * Returns true if this segment is older than the given retention hours.
     * Only inactive segments are eligible for deletion.
     */
    public boolean isExpired(int retentionHours) {
        return !active && createdAt.isBefore(
                Instant.now().minusSeconds(retentionHours * 3600L)
        );
    }

    /**
     * Marks this segment as inactive — no more writes allowed.
     * Called when the segment is rolled.
     */
    public void deactivate() {
        this.active = false;
        log.info("Segment '{}' deactivated", path.getFileName());
    }

    /**
     * Deletes this segment file from disk.
     * Only called by RetentionManager on expired segments.
     */
    public void delete() throws IOException {
        close();
        path.toFile().delete();
        log.info("Segment '{}' deleted", path.getFileName());
    }

    /**
     * Scans the segment file on startup to recover the last written offset.
     */
    private void recoverLastOffset() throws IOException {
        long readPosition = 0;

        while (readPosition + LogEntry.FIXED_HEADER_SIZE <= writePosition) {
            file.seek(readPosition);

            long offset = file.readLong();
            long timestamp = file.readLong();
            int headersLen = file.readInt();
            int payloadLen = file.readInt();

            if (timestamp == 0) {
                break;
            }

            long nextPosition = readPosition + LogEntry.FIXED_HEADER_SIZE + payloadLen + headersLen;
            if (nextPosition > writePosition) {
                break;
            }

            lastOffset = offset;
            readPosition = nextPosition;
        }
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

    public long getBaseOffset() {
        return baseOffset;
    }

    public long getLastOffset() {
        return lastOffset;
    }

    public long getWritePosition() {
        return writePosition;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}