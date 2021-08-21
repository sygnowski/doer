package io.github.s7i.doer.domain.rocksdb;

import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

import java.util.List;

public interface OnRocksDbOpen {

    void onOpen(RocksDB db, List<ColumnFamilyHandle> handles);
}
