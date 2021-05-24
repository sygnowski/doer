package io.github.s7i.doer.config;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Ingest extends Base {

    Map<String, String> kafka;
    IngestSpec ingest;

    @Getter
    public static class IngestSpec {

        Proto proto;
        List<Template> templates;
        @JsonProperty("value_sets")
        List<ValueSet> valueSets;
        List<Topic> topics;

        public ValueSet findValueSet(String valueSetName) {
            return getValueSets()
                  .stream()
                  .filter(vs -> vs.getName().equals(valueSetName))
                  .findFirst().orElseThrow();
        }

        public String findTemplate(ValueTemplate valueTemplate) {
            return getTemplates()
                  .stream()
                  .filter(t -> t.getName().equals(valueTemplate.getTemplateName()))
                  .findFirst()
                  .orElseThrow()
                  .getContent();
        }
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
        Long repeat;

        public Stream<List<String>> stream() {
            requireNonNull(values, "ValueSet::values");
            if (nonNull(repeat) && repeat > 1) {
                var r = LongStream.range(0, repeat)
                      .mapToObj(step -> values.stream())
                      .flatMap(Function.identity());
                return r;

            }
            return values.stream();
        }
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
        List<Header> headers;
        @JsonProperty("value_template")
        ValueTemplate valueTemplate;

        public boolean hasHeaders() {
            return nonNull(headers) && !headers.isEmpty();
        }
    }

    @Getter
    public static class Header {

        String name;
        String value;
    }

    @Getter
    public static class ValueTemplate {

        @JsonProperty("name")
        String templateName;
        @JsonProperty("proto_message")
        String protoMessage;
        List<TemplateProp> properties;

        public List<TemplateProp> getProperties() {
            if (isNull(properties)) {
                return Collections.emptyList();
            }
            return properties;
        }
    }

    @Getter
    public static class TemplateProp {

        String name;
        String value;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Proto implements ProtoDescriptorContainer {

        @JsonProperty("descriptor_set")
        List<String> descriptorSet;
    }
}
