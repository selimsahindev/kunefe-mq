package dev.selimsahin.kunefe.broker.consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Persistent offset store backed by RocksDB.
 * <p>
 * Stores the last committed offset for each consumer group and topic pair.
 * Survives broker restarts — consumers resume from where they left off.
 * <p>
 * Key format:   "{groupId}::{topic}::{consumerId}"
 * Value format: offset as 8-byte long (big-endian)
 */
@Component
public class OffsetStore {

    private static final Logger log = LoggerFactory.getLogger(OffsetStore.class);

    @Value("${kunefe.offset.dir:./kunefe-offsets}")
    private String offsetDir;

    private RocksDB rocks;

    @PostConstruct
    public void init() throws RocksDBException {
        RocksDB.loadLibrary();
        Options options = new Options()
                .setCreateIfMissing(true);
        rocks = RocksDB.open(options, offsetDir);
        log.info("OffsetStore initialized - dir: {}", offsetDir);
    }

    /**
     * Commits the offset for a consumer group, topic, and consumer.
     */
    public void commit(String groupId, String topic, String consumerId, long offset)
            throws RocksDBException {
        byte[] key = buildKey(groupId, topic, consumerId);
        byte[] value = longToBytes(offset);
        rocks.put(key, value);
        log.debug("Committed offset {} for group '{}' on topic '{}'", offset, groupId, topic);
    }

    /**
     * Returns the last committed offset for a consumer group and topic.
     * Returns 0 if no offset has been committed yet — start from beginning.
     */
    public long get(String groupId, String topic, String consumerId) throws RocksDBException {
        byte[] key = buildKey(groupId, topic, consumerId);
        byte[] value = rocks.get(key);
        return value == null ? 0L : bytesToLong(value);
    }

    @PreDestroy
    public void shutdown() {
        if (rocks == null) {
            return;
        }
        rocks.close();
        log.info("OffsetStore closed");
    }

    private byte[] buildKey(String groupId, String topic, String consumerId) {
        String key = groupId + "::" + topic + "::" + consumerId;
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] longToBytes(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    private long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}