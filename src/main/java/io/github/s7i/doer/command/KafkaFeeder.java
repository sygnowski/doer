package io.github.s7i.doer.command;

import static io.github.s7i.doer.Utils.hasAnyValue;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.Tracing;
import io.github.s7i.doer.Utils.PropertyResolver;
import io.github.s7i.doer.config.Ingest;
import io.github.s7i.doer.config.Ingest.Entry;
import io.github.s7i.doer.config.Ingest.IngestSpec;
import io.github.s7i.doer.config.Ingest.TemplateProp;
import io.github.s7i.doer.config.Ingest.ValueSet;
import io.github.s7i.doer.config.Ingest.ValueTemplate;
import io.github.s7i.doer.proto.Decoder;
import io.opentracing.contrib.kafka.TracingKafkaProducer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kfeed")
@Slf4j
public class KafkaFeeder implements Runnable, YamlParser {

    public static KafkaFeeder createCommandInstance(File yaml) {
        var cmd = new KafkaFeeder();
        cmd.yaml = yaml;
        return cmd;
    }

    @Option(names = {"-y", "-yaml"}, defaultValue = "ingest.yml")
    private File yaml;
    private Path root;
    private Decoder decoder;
    @Option(names = "-t", description = "Use Open Tracing")
    private boolean useTracing;

    @Override
    public File getYamlFile() {
        if (!yaml.exists()) {
            throw new IllegalStateException("ingestion file doesn't exists: " + yaml);
        }
        root = yaml.toPath().toAbsolutePath().getParent();
        return yaml;
    }

    @Override
    public void run() {
        var config = parseYaml(Ingest.class);

        var records = produceRecords(config.getIngest());
        log.info("feeding kafka, prepared records count: {}", records.size());

        try (var producer = createProducer(config)) {
            records.stream()
                  .map(FeedRecord::toRecord)
                  .forEach(producer::send);
        }

        log.info("kafka feeder ends");
    }

    private List<FeedRecord> produceRecords(IngestSpec spec) {
        if (nonNull(spec.getProto())) {
            decoder = new Decoder();
            decoder.loadDescriptors(spec.getProto());
        }
        var result = new ArrayList<FeedRecord>();
        for (var topic : spec.getTopics()) {
            buildEntries(spec, result, topic);
        }
        return result;
    }

    private void buildEntries(IngestSpec spec, ArrayList<FeedRecord> result, Ingest.Topic topic) {
        for (var entry : topic.getEntries()) {
            if (isTemplateEntry(topic, entry)) {
                TemplateResolver.builder()
                      .entry(entry)
                      .valueSet(spec.findValueSet(topic.getValueSet()))
                      .template(spec.findTemplate(entry.getValueTemplate()))
                      .decoder(decoder)
                      .build()
                      .topicEntries()
                      .forEach(data -> result.add(new FeedRecord(topic.getName(), data)));
            }
        }
    }

    private boolean isTemplateEntry(Ingest.Topic topic, Entry entry) {
        return hasAnyValue(topic.getValueSet()) && nonNull(entry.getValueTemplate());
    }

    @Builder
    static class TemplateResolver {

        Entry entry;
        ValueSet valueSet;
        String template;
        Decoder decoder;

        public Optional<TopicEntry> makeTopicEntry(RowProcessor rower) {
            var payload = rower.resolve(template);
            var rowKey = entry.getKey();
            var filledKey = rower.resolve(rowKey);

            try {
                var data = asBinaryProto(payload);
                var topicEntry = new TopicEntry(filledKey, data);
                assignHeaders(topicEntry);

                return Optional.of(topicEntry);
            } catch (RuntimeException e) {
                //do nothing
                return Optional.empty();
            }
        }

        private void assignHeaders(TopicEntry topicEntry) {
            if (entry.hasHeaders()) {
                var headers = entry.getHeaders().stream()
                      .map(h -> Header.from(h.getName(), h.getValue()))
                      .collect(Collectors.toList());
                topicEntry.setHeaders(headers);
            }
        }

        private byte[] asBinaryProto(String payload) {
            return decoder.toMessage(findDescriptor(), payload).toByteArray();
        }

        private Descriptor findDescriptor() {
            return decoder.findMessageDescriptor(entry.getValueTemplate().getProtoMessage());
        }

        public Stream<TopicEntry> topicEntries() {
            var rp = new RowProcessor(valueSet.getAttributes());
            return valueSet.stream()
                  .map(rp::nextRowValues)
                  .map(r -> r.updateTemplateProperties(entry.getValueTemplate()))
                  .map(this::makeTopicEntry)
                  .filter(Optional::isPresent)
                  .map(Optional::get);
        }
    }

    @RequiredArgsConstructor
    static class RowProcessor {

        final Map<String, String> rowState = new HashMap<>();
        PropertyResolver resolver = new PropertyResolver(rowState);
        long rowNum;
        final List<String> attributes;

        public String resolve(String input) {
            return resolver.resolve(input);
        }

        RowProcessor updateTemplateProperties(ValueTemplate valueTemplate) {
            requireNonNull(valueTemplate);
            valueTemplate
                  .getProperties()
                  .forEach(this::addProperty);
            return this;
        }

        public void addProperty(TemplateProp prop) {
            resolver.addProperty(prop.getName(), prop.getValue());
        }

        RowProcessor nextRowValues(List<String> values) {
            rowState.put("__#", String.valueOf(++rowNum));
            for (int pos = 0; pos < attributes.size(); pos++) {
                var value = resolver.resolve(values.get(pos));
                rowState.put(attributes.get(pos), value);
            }

            return this;
        }
    }

    @Data
    @AllArgsConstructor
    static class FeedRecord {

        String topic;
        @Delegate
        TopicEntry entry;

        public ProducerRecord<String, byte[]> toRecord() {
            var record = new ProducerRecord(getTopic(), getKey(), getData());
            entry.getHeaders().forEach(h -> record.headers().add(h.getName(), h.getValue()));
            return record;
        }
    }

    @Data
    @RequiredArgsConstructor
    static class TopicEntry {

        List<Header> headers = new ArrayList<>(0);
        final String key;
        final byte[] data;
    }


    @Data
    @AllArgsConstructor
    static class Header {

        static Header from(String name, String value) {
            requireNonNull(name, "name");
            requireNonNull(value, "value");
            return new Header(name, value.getBytes(StandardCharsets.UTF_8));
        }

        String name;
        byte[] value;
    }


    private String asText(Path path) {
        try {
            var relative = root;
            relative = relative.resolve(path);
            return Files.readString(relative);
        } catch (IOException e) {
            KafkaFeeder.log.error("", e);
            throw new RuntimeException(e);
        }
    }

    private Producer<String, byte[]> createProducer(Ingest ingest) {
        var props = new Properties();
        props.putAll(ingest.getKafka());
        var producer = new KafkaProducer<String, byte[]>(props);
        if (useTracing) {
            return new TracingKafkaProducer(producer, Tracing.INSTANCE.getTracer());
        }
        return producer;
    }
}
