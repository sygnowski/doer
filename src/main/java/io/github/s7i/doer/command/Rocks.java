package io.github.s7i.doer.command;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.github.s7i.doer.domain.rocksdb.RocksDb;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rocks")
@Slf4j
public class Rocks implements Runnable {

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

    RocksDb rocksdb;

    @Override
    public void run() {
        rocksdb = new RocksDb(dbPath);
        switch (action) {
            case "put":
                put();
                break;
            case "get":
                get();
                break;
            case "list":
                list();
                break;
            default:
                log.info("bad action");
                break;
        }
    }

    private void list() {
        readOnly();
        if (nonNull(colFamilyName)) {
            rocksdb.readAsString(colFamilyName)
                  .forEach(e -> log.info("k: {}, v: {}", e.getKey(), e.getValue()));
        } else {
            log.info("columns families: {}", rocksdb.listColumns());
        }
    }

    private void get() {
        if (isNull(key)) {
            list();
        } else {
            readOnly();
            String value = rocksdb.getAsString(name(), key);
            log.info("v: {}", value);
        }
    }

    private void readOnly() {
        rocksdb.setReadOnly(true);
        rocksdb.setCreateIfMissing(false);
        rocksdb.setCreateMissingColumnFamilies(false);
    }

    private void put() {
        rocksdb.put(name(), key, value);
    }

    private String name() {
        return nonNull(colFamilyName) ? colFamilyName : RocksDb.DEFAULT_COLUMN_FAMILY;
    }
}
