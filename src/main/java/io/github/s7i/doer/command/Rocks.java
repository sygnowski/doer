package io.github.s7i.doer.command;

import io.github.s7i.doer.domain.rocksdb.RocksDb;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;

import static java.util.Objects.nonNull;

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
                case "list":
                    if (nonNull(colFamilyName)) {
                        new RocksDb(dbPath)
                                .readAsString(colFamilyName)
                                .forEach(e -> log.info("entry {}", e));
                    } else {
                        log.info("columns families: {}", new RocksDb(dbPath).listColumns());
                    }
                    break;
                default:
                    log.info("bad action");
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
        new RocksDb(dbPath).put(colFamilyName, key, value);
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
