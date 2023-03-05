package io.github.s7i.doer.domain.rocksdb.dataview;

import io.github.s7i.doer.domain.rocksdb.Key;

import java.nio.charset.StandardCharsets;

public class PlainString implements Key {

    public String readKey(byte[] data) {
        return new String(data);
    }


    public byte[] writeKey(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getKeyData() {
        return new byte[0];
    }
}
