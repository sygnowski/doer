package io.github.s7i.doer.domain.output;

import static java.util.Objects.nonNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.StringValue;
import io.github.s7i.doer.proto.Record;
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

        public Record toRecord() {
            var b = Record.newBuilder();
            if (nonNull(resource)) {
                b.setResource(StringValue.of(resource));
            }
            if (nonNull(key)) {
                b.setKey(StringValue.of(key));
            }
            if (nonNull(meta)) {
                b.putAllMeta(meta);
            }
            if (nonNull(data) && data.length > 0) {
                b.setData(BytesValue.of(ByteString.copyFrom(data)));
            }
            return b.build();
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
