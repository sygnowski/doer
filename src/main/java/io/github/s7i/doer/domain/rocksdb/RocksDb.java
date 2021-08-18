package io.github.s7i.doer.domain.rocksdb;

import lombok.RequiredArgsConstructor;
import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RocksDb {
    static {
        RocksDB.loadLibrary();
    }

    private final String dbPath;

    public List<String> listColumns() {
        var options = new Options();
        try {
            var list = RocksDB.listColumnFamilies(options, dbPath);
            return list.stream().map(String::new).collect(Collectors.toUnmodifiableList());

        } catch (RocksDBException e) {
            throw new RocksDbRuntimeException(e);
        }
    }

    public void put(String name, String key, String value) {
        final var columns = new ArrayList<String>();
        columns.addAll(listColumns());
        if (!columns.contains(name)) {
            initColumnFamilies(columns, List.of(name));
            columns.add(name);
        }

        open(columns, (db, hnds) -> RocksDbUtil.put(db, hnds.get(1), key, value));
    }

    public void open(List<String> columnFamilyNames, OnRocksDbOpen onOpen) {
        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = new DBOptions()) {
            var descriptors = new ArrayList<ColumnFamilyDescriptor>();
            descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));

            for (var cf : columnFamilyNames) {
                descriptors.add(new ColumnFamilyDescriptor(cf.getBytes(), new ColumnFamilyOptions()));
            }

            var db = RocksDB.open(option, dbPath, descriptors, handlers);

            try {
                onOpen.onOpen(db, handlers);

            } finally {
                handlers.forEach(ColumnFamilyHandle::close);
            }
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        }
    }


    public void initColumnFamilies(List<String> existing, List<String> names) {
        var descriptors = existing.stream()
                .map(c -> new ColumnFamilyDescriptor(c.getBytes(), new ColumnFamilyOptions()))
                .collect(Collectors.toList());

        var handles = new ArrayList<ColumnFamilyHandle>();
        try (var options = new DBOptions().setCreateIfMissing(true)) {
            var db = RocksDB.open(options, dbPath, descriptors, handles);
            for (var name : names) {
                var handle = db.createColumnFamily(new ColumnFamilyDescriptor(
                        name.getBytes(),
                        new ColumnFamilyOptions()
                ));
                handle.close();
            }
            db.close();
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        } finally {
            handles.forEach(ColumnFamilyHandle::close);
        }
    }

}
