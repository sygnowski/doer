package io.github.s7i.doer.domain.output;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
    }

    void open();

    default void emit(String resource, byte[] data) {
        emit(Load.builder()
              .resource(resource)
              .data(data)
              .build());
    }

    void emit(Load load);

}
