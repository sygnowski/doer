package io.github.s7i.doer.command;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;

@Command(name = "rocks")
@Slf4j
public class Rocks implements Runnable {

    static {
        RocksDB.loadLibrary();
    }


    @Option(names = "-db", required = true)
    String dbPath;

    @Option(names = "-a", required = true)
    String action;

    @Option(names = "-cf")
    String colFamilyName;

    @Option(names = "-k")
    String key;

    @Option(names = "-v")
    String value;

    @Override
    public void run() {
        try {
            switch (action) {
                case "init":
                    init();
                    break;
                case "put":
                    put();
                    break;
                case "get":
                    get();
                    break;
            }
        } catch (RocksDBException rex) {
            log.error("rocksdb", rex);
        }

    }

    private void get() throws RocksDBException {

        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = new DBOptions()) {
            var descriptors = new ArrayList<ColumnFamilyDescriptor>();
            descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
            descriptors.add(new ColumnFamilyDescriptor(colFamilyName.getBytes(), new ColumnFamilyOptions()));

            var db = RocksDB.open(option, dbPath, descriptors, handlers);

            try {
                var byteValue = db.get(handlers.get(1), key.getBytes());
                log.info("value: {}", byteValue == null ? "[null]" : new String(byteValue));

            } finally {
                handlers.forEach(ColumnFamilyHandle::close);
            }
        }


    }

    private void put() throws RocksDBException {
        var handlers = new ArrayList<ColumnFamilyHandle>();
        try (var option = new DBOptions()) {
            var descriptors = new ArrayList<ColumnFamilyDescriptor>();
            descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, new ColumnFamilyOptions()));
            descriptors.add(new ColumnFamilyDescriptor(colFamilyName.getBytes(), new ColumnFamilyOptions()));

            var db = RocksDB.open(option, dbPath, descriptors, handlers);

            try {
                db.put(handlers.get(1), new WriteOptions(), key.getBytes(), value.getBytes());

            } finally {
                handlers.forEach(ColumnFamilyHandle::close);
            }
        }
    }

    private void init() throws RocksDBException {
        try (var options = new Options().setCreateIfMissing(true)) {

            var db = RocksDB.open(options, dbPath);
            var handle = db.createColumnFamily(new ColumnFamilyDescriptor(
                    colFamilyName.getBytes(),
                    new ColumnFamilyOptions()
            ));
            handle.close();
        }
    }
}
