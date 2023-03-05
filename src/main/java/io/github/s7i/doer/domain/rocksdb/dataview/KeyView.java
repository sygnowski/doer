package io.github.s7i.doer.domain.rocksdb.dataview;

import io.github.s7i.doer.domain.rocksdb.Key;

import java.util.EnumMap;
import java.util.Map;

public class KeyView implements Key {

    @Override
    public byte[] getKeyData() {
        return new byte[0];
    }

    enum Kind {
        PLAIN, FLINK
    }

    Kind defaultKind = Kind.FLINK;
    static final Map<Kind, Key> map = new EnumMap<>(Kind.class);

    static {
        map.put(Kind.PLAIN, new PlainString());
        map.put(Kind.FLINK, new FlinkKey());
    }

    public String readKey(byte[] data) {
        return byKind().readKey();
    }

    private Key byKind() {
        return map.get(defaultKind);
    }

    public byte[] writeKey(String data) {
//        return byKind().writeKey(null , null);
        return new byte[0];
    }
}
