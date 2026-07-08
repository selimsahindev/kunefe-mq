package dev.selimsahin.kunefe.broker.consumer;

import org.rocksdb.RocksDBException;

/**
 * Port interface for offset store operations.
 */
public interface OffsetStorePort {

    void commit(String groupId, String topic, String consumerId, long offset) throws RocksDBException;

    long get(String groupId, String topic, String consumerId) throws RocksDBException;
}