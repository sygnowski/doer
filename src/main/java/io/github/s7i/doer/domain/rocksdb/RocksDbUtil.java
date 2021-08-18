package io.github.s7i.doer.domain.rocksdb;

import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class RocksDbUtil {

    public static void put(RocksDB rocks, ColumnFamilyHandle columnFamilyHandle, String key, String value) {
        put(rocks, columnFamilyHandle, new WriteOptions(), key.getBytes(), value.getBytes());
    }

    public static void put(RocksDB rocks, ColumnFamilyHandle columnFamilyHandle, WriteOptions writeOpts, byte[] key, byte[] value) {
        try {
            rocks.put(columnFamilyHandle, writeOpts, key, value);
        } catch (RocksDBException e) {
            throw new RocksDbRuntimeException(e);
        }
    }
}
