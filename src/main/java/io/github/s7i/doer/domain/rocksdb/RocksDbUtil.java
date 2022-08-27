package io.github.s7i.doer.domain.rocksdb;

import java.util.List;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;

public class RocksDbUtil {

    public static ColumnFamilyHandle findHandler(List<ColumnFamilyHandle> handles, String columnName) {
        return handles.stream()
              .filter(a -> RocksDbUtil.getName(a).equals(columnName))
              .findFirst().orElseThrow();
    }

    public static String getName(ColumnFamilyHandle handle) {
        try {
            return new String(handle.getName());
        } catch (RocksDBException e) {
            throw new RocksDbRuntimeException(e);
        }
    }

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
