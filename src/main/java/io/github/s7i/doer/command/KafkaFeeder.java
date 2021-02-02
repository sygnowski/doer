package io.github.s7i.doer.command;

import static io.github.s7i.doer.Utils.hasAnyValue;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.Utils.PropertyResolver;
import io.github.s7i.doer.config.Ingest;
import io.github.s7i.doer.config.Ingest.Entry;
import io.github.s7i.doer.config.Ingest.IngestSpec;
import io.github.s7i.doer.config.Ingest.TemplateProp;
import io.github.s7i.doer.config.Ingest.ValueTemplate;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
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
            if (hasAnyValue(topic.getValueSet()) && nonNull(entry.getValueTemplate())) {
                fillTemplate(spec, entry, topic.getValueSet())
                      .forEach(data -> result.add(new FeedRecord(topic.getName(), data)));
            }
        }
    }

    private List<TopicEntry> fillTemplate(IngestSpec spec, Entry entry, String valueSetName) {
        final var valueTemplate = entry.getValueTemplate();
        final var descriptor = decoder.findMessageDescriptor(valueTemplate.getProtoMessage());

        final var template = spec.getTemplates()
              .stream()
              .filter(t -> t.getName().equals(valueTemplate.getTemplateName()))
              .findFirst()
              .orElseThrow()
              .getContent();

        final var valueSet = spec.getValueSets()
              .stream()
              .filter(vs -> vs.getName().equals(valueSetName))
              .findFirst().orElseThrow();

        final Function<RowProcessor, Optional<TopicEntry>> templater = rower -> {
            var payload = rower.resolve(template);
            var rowKey = entry.getKey();
            var filledKey = rower.resolve(rowKey);

            try {
                var data = decoder.toMessage(descriptor, payload).toByteArray();
                return Optional.of(new TopicEntry(filledKey, data));
            } catch (RuntimeException e) {
                //do nothing
                return Optional.empty();
            }
        };

        final var rower = new RowProcessor(valueSet.getAttributes());

        var result = valueSet.stream()
              .map(v -> rower.nextRowValues(v))
              .map(r -> r.updateTemplateProperties(valueTemplate))
              .map(templater::apply)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

        return result;
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
            return new ProducerRecord(getTopic(), getKey(), getData());
        }
    }

    @Data
    @AllArgsConstructor
    static class TopicEntry {

        String key;
        byte[] data;
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

    private KafkaProducer<String, byte[]> createProducer(Ingest ingest) {
        var props = new Properties();
        props.putAll(ingest.getKafka());
        return new KafkaProducer<>(props);
    }
}
