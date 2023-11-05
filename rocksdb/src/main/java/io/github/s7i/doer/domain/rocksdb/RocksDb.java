package io.github.s7i.doer.domain.rocksdb;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.rocksdb.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class RocksDb {

    public static final String DEFAULT_COLUMN_FAMILY = new String(RocksDB.DEFAULT_COLUMN_FAMILY);
    public static final ColumnFamilyDescriptor DEFAULT = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY);

    static {
        RocksDB.loadLibrary();
    }

    private final String dbPath;
    private final Map<String, ColumnFamilyOptions> cfOptions = Collections.emptyMap();
    @Setter
    private boolean createIfMissing = true;
    @Setter
    private boolean createMissingColumnFamilies = true;
    @Setter
    private boolean readOnly;

    public List<String> listColumns() {
        return listColumns(false);
    }

    protected List<String> listColumns(boolean addDefaultIfMissing) {
        var options = new Options();
        try {
            var list = RocksDB.listColumnFamilies(options, dbPath);
            if (list.isEmpty() && addDefaultIfMissing) {
                return List.of(DEFAULT_COLUMN_FAMILY);
            }
            return list.stream().map(String::new).collect(Collectors.toUnmodifiableList());

        } catch (RocksDBException e) {
            throw new RocksDbRuntimeException(e);
        }
    }

    public void put(String name, String key, String value) {
        var columns = initColumnFamilies(name);

        open(columns, (db, hnds) -> RocksDbUtil.put(
              db,
              hnds.get(columns.indexOf(name)),
              key,
              value
        ));
    }

    public Optional<String> getAsString(String colFamilyName, String key) {
        var getter = new OnRocksDbOpen() {
            String val;

            @Override
            public void onOpen(RocksDB db, List<ColumnFamilyHandle> handles) {
                try {
                    var handler = RocksDbUtil.findHandler(handles, colFamilyName);
                    var byteValue = db.get(handler, key.getBytes());
                    if (byteValue != null) {
                        val = new String(byteValue);
                    }
                } catch (RocksDBException e) {
                    throw new RocksDbRuntimeException(e);
                }
            }
        };
        open(getter);
        return Optional.ofNullable(getter.val);
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
                var hnd = RocksDbUtil.findHandler(h, column);
                try (var it = db.newIterator(hnd)) {
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

    protected void open(List<String> columnFamilyNames, OnRocksDbOpen onOpenComplete) {
        open(columnFamilyNames, (OnRocksDbOpenComplete) onOpenComplete);
    }

    protected void open(List<String> columnFamilyNames, OnRocksDbOpenComplete onOpenComplete) {
        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = newOptions()) {
            var descriptors = doDescriptors(columnFamilyNames);
            var db = open(option, descriptors, handlers);
            onOpenComplete.onOpen(db, handlers, () -> {
                handlers.forEach(ColumnFamilyHandle::close);
                db.close();
            });
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        }
    }

    private RocksDB open(DBOptions option, List<ColumnFamilyDescriptor> descriptors, List<ColumnFamilyHandle> handlers) throws RocksDBException {
        var db = readOnly
              ? RocksDB.openReadOnly(option, dbPath, descriptors, handlers)
              : RocksDB.open(option, dbPath, descriptors, handlers);
        return db;
    }

    public List<String> initColumnFamilies(String... names) {
        if (nonNull(names) && names.length > 0) {
            var existing = listColumns(true);

            open(existing, (db, handles) -> Arrays.stream(names)
                  .filter(n -> !existing.contains(n))
                  .forEach(n -> create(db, n))
            );
        }
        return listColumns();
    }

    private void create(RocksDB db, String name) {
        try {
            var handle = db.createColumnFamily(newDescriptor(name));
            handle.close();
        } catch (RocksDBException rex) {
            throw new RocksDbRuntimeException(rex);
        }
    }

    private DBOptions newOptions() {
        return new DBOptions()
              .setCreateIfMissing(createIfMissing)
              .setCreateMissingColumnFamilies(createMissingColumnFamilies);
    }

    private ColumnFamilyDescriptor newDescriptor(String name) {
        return name.equals(DEFAULT_COLUMN_FAMILY)
              ? DEFAULT
              : new ColumnFamilyDescriptor(
                    name.getBytes(),
                    cfOptions.getOrDefault(name, new ColumnFamilyOptions())
              );
    }

    private List<ColumnFamilyDescriptor> doDescriptors(List<String> names) {
        var descriptors = names.stream()
              .map(this::newDescriptor)
              .collect(Collectors.toList());
        return descriptors;
    }

}
