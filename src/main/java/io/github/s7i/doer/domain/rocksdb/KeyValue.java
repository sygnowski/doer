package io.github.s7i.doer.domain.rocksdb;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class KeyValue <K extends Key, V> {
    K key;
    V value;
}
