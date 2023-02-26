package io.github.s7i.doer.domain.output;

import io.github.s7i.doer.domain.Mappers;
import io.github.s7i.doer.proto.Record;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

public interface Output extends AutoCloseable {

    @Builder
    @AllArgsConstructor
    @Data
    class Load {

        String resource;
        String key;
        Map<String, String> meta;
        byte[] data;

        public String dataAsString() {
            return new String(data);
        }

        public Record toRecord() {
            return Mappers.mapFrom(this);
        }
    }

    void open();

    default void emit(String resource, byte[] data) {
        emit(Mappers.mapFrom(resource, data));
    }

    void emit(Load load);

}
