package io.github.s7i.doer.config;

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
    IngestSpec ingest;

    @Getter
    public static class IngestSpec {

        Proto proto;
        Template template;
        @JsonProperty("value_sets")
        List<ValueSet> valueSets;
        List<Topic> topics;
    }

    @Getter
    public static class Template {

        String name;
        String content;
    }

    @Getter
    public static class ValueSet {

        String name;
        List<String> attributes;
        List<List<String>> values;
    }

    @Getter
    public static class Topic {

        String name;
        @JsonProperty("value_set")
        String valueSet;
        List<Entry> entries;
    }

    @Getter
    public static class Entry {

        String key;
        ValueTemplate valueTemplate;
    }

    @Getter
    public static class ValueTemplate {

        @JsonProperty("name")
        String templateName;
        @JsonProperty("proto_message")
        String protoMessage;
        List<TemplateProp> properties;
    }

    @Getter
    public static class TemplateProp {

        String name;
        String value;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Proto {

        @JsonProperty("descriptor_set")
        List<String> descriptorSet;
    }
}
