package io.github.s7i.doer.command;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.domain.rocksdb.RocksDb;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
        var rocksdb = new RocksDb(dbPath);
        try {
            switch (action) {
                case "put":
                    put(rocksdb);
                    break;
                case "get":
                    get(rocksdb);
                    break;
                case "list":
                    if (nonNull(colFamilyName)) {
                        rocksdb.readAsString(colFamilyName)
                              .forEach(e -> log.info("k: {}, v: {}", e.getKey(), e.getValue()));
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

    private void get(RocksDb rocksDb) throws RocksDBException {
        String value = rocksDb.getAsString(colFamilyName, key);
        log.info("v: {}", value);
    }

    private void put(RocksDb rocksDb) throws RocksDBException {
        rocksDb.put(colFamilyName, key, value);
    }
}
