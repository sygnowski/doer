package io.github.s7i.doer.domain.rocksdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

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

        open(columns, (db, hnds) -> RocksDbUtil.put(
              db,
              hnds.get(columns.indexOf(name)),
              key,
              value
        ));
    }

    public String getAsString(String colFamilyName, String key) {
        var getter = new OnRocksDbOpen() {
            String val;

            @Override
            public void onOpen(RocksDB db, List<ColumnFamilyHandle> handles) {
                try {
                    var handler = RocksDbUtil.findHandler(handles, colFamilyName);
                    var byteValue = db.get(handler, key.getBytes());
                    val = new String(byteValue);
                } catch (RocksDBException e) {
                    throw new RocksDbRuntimeException(e);
                }
            }
        };
        open(getter);
        return getter.val;
    }

    @RequiredArgsConstructor
    class DbIterator implements Iterator<KeyValue<String, String>>, Iterable<KeyValue<String, String>>, OnRocksDbOpenComplete {

        final String column;
        private RocksIterator iterator;
        Complete complete;

        @Override
        public Iterator<KeyValue<String, String>> iterator() {
            open(listColumns(), this);
            return this;
        }

        @Override
        public void onOpen(RocksDB db, List<ColumnFamilyHandle> handles, Complete complete) {
            this.complete = complete;

            var hnd = handles.stream()
                  .filter(a -> RocksDbUtil.getName(a).equals(column))
                  .findFirst().orElseThrow();

            iterator = db.newIterator(hnd);
            iterator.seekToFirst();
        }

        @Override
        public boolean hasNext() {
            iterator.next();
            var haxNext = iterator.isValid();
            try {
                iterator.status();
            } catch (RocksDBException rex) {
                throw new RocksDbRuntimeException(rex);
            }
            if (!haxNext) {
                complete.complete();
            }
            return haxNext;
        }

        @Override
        public KeyValue<String, String> next() {
            var kv = new KeyValue<String, String>();
            kv.setKey(new String(iterator.key()));
            kv.setValue(new String(iterator.value()));

            return kv;
        }
    }

    public Iterable<KeyValue<String, String>> iterableOnAll(String column) {
        return new DbIterator(column);
    }

    public List<KeyValue<String, String>> readAsString(String column) {

        var fetched = new ArrayList<KeyValue<String, String>>();
        open(listColumns(), (db, h) -> {
            try {
                var hnd = h.stream()
                      .filter(a -> RocksDbUtil.getName(a).equals(column))
                      .findFirst().orElseThrow();

                try (var it = db.newIterator()) {
                    for (it.seekToFirst(); it.isValid(); it.next()) {
                        it.status();
                        var key = new String(it.key());
                        var val = new String(it.value());

                        var kv = new KeyValue<String, String>();
                        kv.setKey(key);
                        kv.setValue(val);
                        fetched.add(kv);
                    }
                }

            } catch (RocksDBException rex) {
                throw new RocksDbRuntimeException(rex);
            }
        });
        return fetched;
    }

    public void open(OnRocksDbOpen onRocksDbOpen) {
        open(listColumns(), onRocksDbOpen);
    }

    protected void open(List<String> columnFamilyNames, OnRocksDbOpen onOpen) {
        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = new DBOptions().setCreateIfMissing(true)) {
            var descriptors = doDescriptors(columnFamilyNames);
            var db = RocksDB.open(option, dbPath, descriptors, handlers);

            try {
                onOpen.onOpen(db, handlers);

            } finally {
                handlers.forEach(ColumnFamilyHandle::close);
                db.close();
            }
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        }
    }

    protected void open(List<String> columnFamilyNames, OnRocksDbOpenComplete onOpenComplete) {
        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = new DBOptions().setCreateIfMissing(true)) {
            var descriptors = doDescriptors(columnFamilyNames);
            var db = RocksDB.open(option, dbPath, descriptors, handlers);
            onOpenComplete.onOpen(db, handlers, () -> {
                handlers.forEach(ColumnFamilyHandle::close);
                db.close();
            });
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        }
    }

    public void initColumnFamilies(List<String> existing, List<String> names) {
        var descriptors = doDescriptors(existing);

        var handles = new ArrayList<ColumnFamilyHandle>();
        try (var options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
            try (var db = RocksDB.open(options, dbPath, descriptors, handles)) {
                for (var name : names) {
                    var handle = db.createColumnFamily(newDescriptor(name));
                    handle.close();
                }
            }
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        } finally {
            handles.forEach(ColumnFamilyHandle::close);
        }
    }

    private ColumnFamilyDescriptor newDescriptor(String name) {
        return new ColumnFamilyDescriptor(name.getBytes(), new ColumnFamilyOptions());
    }

    private List<ColumnFamilyDescriptor> doDescriptors(List<String> names) {
        var descriptors = names.stream()
              .map(this::newDescriptor)
              .collect(Collectors.toList());
        return descriptors;
    }

}
