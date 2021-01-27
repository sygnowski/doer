package io.github.s7i.doer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dump extends Base {

    Map<String, String> kafka;
    Specs dump;

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Specs {

        @JsonProperty("pool_timeout_sec")
        Integer poolTimeoutSec;
        List<Topic> topics;
        ProtoSpec proto;
    }

    @Getter
    public static class Topic {

        @JsonProperty("show_binary")
        boolean showBinary;
        String name;
        String range;
        Value value;
        String output;
    }

    @Getter
    public static class Value {

        @JsonProperty("proto_message")
        String protoMessage;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    public static class ProtoSpec implements ProtoDescriptorContainer {

        @JsonProperty("descriptor_set")
        List<String> descriptorSet;
    }

}
