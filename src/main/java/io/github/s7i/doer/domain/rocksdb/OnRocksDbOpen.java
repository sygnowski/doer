package io.github.s7i.doer.domain.rocksdb;

import java.util.List;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

public interface OnRocksDbOpen extends OnRocksDbOpenComplete {

    void onOpen(RocksDB db, List<ColumnFamilyHandle> handles);

    @Override
    default void onOpen(RocksDB db, List<ColumnFamilyHandle> handles, Complete complete) {
        onOpen(db, handles);
        complete.complete();
    }
}
