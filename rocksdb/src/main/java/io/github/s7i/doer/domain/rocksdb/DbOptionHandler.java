package io.github.s7i.doer.domain.rocksdb;

import org.rocksdb.DBOptions;

public interface DbOptionHandler {

    DBOptions handleOptions(DBOptions options);

}
