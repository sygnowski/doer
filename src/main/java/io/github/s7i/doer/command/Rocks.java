package io.github.s7i.doer.command;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.domain.rocksdb.RocksDb;
import io.github.s7i.doer.shade.flink.StringValue;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rocks")
public class Rocks implements Runnable, ConsoleLog {

    enum Action {
        PUT, GET, LIST, SCAN
    }

    @Option(names = "-db", required = true)
    String dbPath;

    @Option(names = "-a", defaultValue = "get", description = "Actions: ${COMPLETION-CANDIDATES}")
    Action action;

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
            case PUT:
                put();
                break;
            case GET:
                get();
                break;
            case LIST:
                list();
                break;
            case SCAN:
                scan();
            default:
                info("bad action");
                break;
        }
    }

    private void list() {
        readOnly();
        if (nonNull(colFamilyName)) {
            rocksdb.readAsString(colFamilyName)
                  .forEach(e -> info("k: {}, v: {}", e.getKey(), e.getValue()));
        } else {
            info("columns families: {}", rocksdb.listColumns());
        }
    }

    private void get() {
        if (isNull(key)) {
            list();
        } else {
            readOnly();
            String value = rocksdb.getAsString(name(), key).orElse("");
            info("v: {}", value);
        }
    }

    private void readOnly() {
        rocksdb.setReadOnly(true);
        rocksdb.setCreateIfMissing(false);
        rocksdb.setCreateMissingColumnFamilies(false);
    }

    private void scan() {
        readOnly();
        for (var cfName : rocksdb.listColumns()) {
            for (var it : rocksdb.iterableOnAll(cfName)) {
                byte[] data;

                var key = it.getKey();
                var val = it.getValue();

                if (key != null) {
                    data = key.array();
                    System.out.println("KEY RAW: " + new String(data));
                    System.out.println("KEY FLINK: " + readFlinkString(data));
                }

                if (val != null) {
                    data = val.array();
                    System.out.println("VALUE RAW: " + new String(data));
                    System.out.println("VALUE FLINK: " + readFlinkString(data));
                }

                System.out.println("---");

            }
        }

    }

    @Nullable
    private static String readFlinkString(byte[] keyData) {
        try {
            return StringValue.readString(new DataInputStream(new ByteArrayInputStream(keyData)));
        } catch (IOException e) {
            return "";
        }
    }

    private void put() {
        rocksdb.put(name(), key, value);
    }

    private String name() {
        return nonNull(colFamilyName) ? colFamilyName : RocksDb.DEFAULT_COLUMN_FAMILY;
    }
}
