package io.github.s7i.doer.domain.rocksdb;

import java.util.List;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

public interface OnRocksDbOpenComplete {

    interface Complete {

        void complete();
    }

    void onOpen(RocksDB db, List<ColumnFamilyHandle> handles, Complete complete);
}
