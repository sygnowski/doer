package io.github.s7i.doer.command;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.manifest.ingest.Entry;
import io.github.s7i.doer.manifest.ingest.Ingest;
import io.github.s7i.doer.manifest.ingest.IngestManifest;
import io.github.s7i.doer.manifest.ingest.TemplateProp;
import io.github.s7i.doer.manifest.ingest.Topic;
import io.github.s7i.doer.manifest.ingest.ValueSet;
import io.github.s7i.doer.manifest.ingest.ValueTemplate;
import io.github.s7i.doer.proto.Decoder;
import io.github.s7i.doer.util.PropertyResolver;
import io.github.s7i.doer.util.SpecialExpression;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kfeed")
@Slf4j
public class KafkaFeeder implements Runnable, YamlParser {

    public static KafkaFactory kafka = new KafkaFactory();

    public static KafkaFeeder createCommandInstance(File yaml) {
        var cmd = new KafkaFeeder();
        cmd.yaml = yaml;
        return cmd;
    }

    @Option(names = {"-y", "-yaml"}, defaultValue = "ingest.yml")
    protected File yaml;
    protected Path root;
    protected Decoder decoder;
    @Option(names = "-t", description = "Use Open Tracing")
    protected boolean useTracing;
    @Option(names = "-l", description = "Allowed Labels")
    protected List<String> allowedLabels;

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

        try (var producer = kafka.getProducerFactory().createProducer(config, useTracing)) {
            records.stream()
                  .map(FeedRecord::toRecord)
                  .forEach(producer::send);
        }

        log.info("kafka feeder ends");
    }

    private List<FeedRecord> produceRecords(IngestManifest spec) {
        if (nonNull(spec.getProto())) {
            decoder = new Decoder();
            decoder.loadDescriptors(spec.getProto());
        }
        var result = new ArrayList<FeedRecord>();
        for (var topic : spec.getTopics()) {
            if (isAllowed(topic)) {
                buildEntries(spec, result, topic);
            }
        }
        return result;
    }

    private boolean isAllowed(Topic topic) {
        if (nonNull(allowedLabels)) {
            return allowedLabels.contains(topic.getLabel());
        }
        return true;
    }

    protected void buildEntries(IngestManifest spec, ArrayList<FeedRecord> result, Topic topic) {
        for (var entry : topic.getEntries()) {
            if (entry.isTemplateEntry()) {
                TemplateResolver.builder()
                      .entry(entry)
                      .valueSet(spec.findValueSet(topic.getValueSet()))
                      .template(spec.findTemplate(entry.getValueTemplate()))
                      .decoder(decoder)
                      .build()
                      .topicEntries()
                      .forEach(data -> result.add(new FeedRecord(topic.getName(), data)));
            } else if (entry.isSimpleValue()) {
                result.add(FeedRecord.fromSimpleEntry(entry, topic));
            }
        }
    }

    static void assignHeaders(Entry entry, TopicEntry topicEntry) {
        if (entry.hasHeaders()) {
            var headers = entry.getHeaders().stream()
                  .map(h -> Header.from(h.getName(), h.getValue()))
                  .collect(Collectors.toList());
            topicEntry.setHeaders(headers);
        }
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
                byte[] data;
                if (entry.isProto()) {
                    data = asBinaryProto(payload);
                } else {
                    data = payload.getBytes(StandardCharsets.UTF_8);
                }
                var topicEntry = new TopicEntry(filledKey, data);
                assignHeaders(entry, topicEntry);

                return Optional.of(topicEntry);
            } catch (RuntimeException e) {
                //do nothing
                return Optional.empty();
            }
        }

        private byte[] asBinaryProto(String payload) {
            return decoder.toMessage(findDescriptor(), payload).toByteArray();
        }

        private Descriptor findDescriptor() {
            return decoder.findMessageDescriptor(entry.getValueTemplate().getProtoMessage());
        }

        public Stream<TopicEntry> topicEntries() {
            List<String> attributes;
            Stream<List<String>> stream;
            if (ValueSet.EMPTY == valueSet) {
                attributes = entry.getValueTemplate()
                      .getProperties()
                      .stream()
                      .map(TemplateProp::getName)
                      .collect(Collectors.toList());

                var list = entry.getValueTemplate()
                      .getProperties()
                      .stream()
                      .map(TemplateProp::getValue)
                      .collect(Collectors.toList());

                stream = Stream.of(list);
            } else {
                attributes = valueSet.getAttributes();
                stream = valueSet.stream();
            }
            var rp = new RowProcessor(attributes);
            return stream
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
            rowState.put(SpecialExpression.ROW_ID, String.valueOf(++rowNum));
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

        public static FeedRecord fromSimpleEntry(Entry entry, Topic topic) {

            var resolver = new PropertyResolver();
            var topicName = topic.getName();
            var key = entry.getKey() != null
                  ? resolver.resolve(entry.getKey())
                  : null;
            var simpleValue = resolver.resolve(entry.getSimpleValue());
            var data = simpleValue.getBytes(StandardCharsets.UTF_8);
            var topicEntry = new TopicEntry(key, data);
            assignHeaders(entry, topicEntry);
            return new FeedRecord(topicName, topicEntry);
        }

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
}
