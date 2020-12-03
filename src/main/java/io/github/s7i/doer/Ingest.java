package io.github.s7i.doer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ingest {

    String version;
    String type;
    Map<String, String> kafka;
    List<IngestLine> ingest;

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class IngestLine {

        String topic;
        String key;
        Value value;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Value {

        String text;
        Proto proto;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Proto {

        @JsonProperty("message_name")
        String messageName;
        @JsonProperty("descriptor_set")
        String[] descriptorSet;
        String json;
    }
}
