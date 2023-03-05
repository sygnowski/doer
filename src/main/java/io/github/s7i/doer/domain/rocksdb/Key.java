package io.github.s7i.doer.domain.rocksdb;

import io.github.s7i.doer.DoerException;

import java.util.function.Consumer;

public interface Key {

    byte[] getKeyData();

    default String readKey() {
        return "";

    }

    default void writeKey(Consumer<byte[]> hnd, String data) {
        throw new DoerException("NYI");
    }
}
