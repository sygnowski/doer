package io.github.s7i.doer.config;

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
    Specs dump;

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Specs {

        @JsonProperty("show_binary")
        boolean showBinary;
        @JsonProperty("show_proto")
        boolean showProto;
        @JsonProperty("pool_timeout_sec")
        Integer poolTimeoutSec;
        String topic;
        String output;
        ProtoSpec proto;
        String range;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    public static class ProtoSpec {

        @JsonProperty("message_name")
        String messageName;

        @JsonProperty("descriptor_set")
        String[] descriptorSet;
    }

}
