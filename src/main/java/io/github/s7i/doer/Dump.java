package io.github.s7i.doer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dump {

    String version;
    String type;
    Map<String, String> kafka;
    DumpEntry dump;

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DumpEntry {

        @JsonProperty("show_binary")
        boolean showBinary;
        @JsonProperty("show_proto")
        boolean showProto;
        @JsonProperty("pool_timeout_sec")
        Integer poolTimeoutSec;
        String topic;
        String output;
        ProtoEntry proto;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    public static class ProtoEntry {

        @JsonProperty("message_name")
        String messageName;

        @JsonProperty("descriptor_set")
        String[] descriptorSet;
    }

}
