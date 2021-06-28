package io.github.s7i.doer.command.dump;


import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.config.Dump;
import io.github.s7i.doer.config.Dump.Topic;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.OutputFactory;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kdump")
@Slf4j
public class KafkaDump implements Runnable, YamlParser {

    public static KafkaFactory kafka = new KafkaFactory();
    public static OutputFactory outputFactory = new OutputFactory();

    public static KafkaDump createCommandInstance(File yaml) {
        var cmd = new KafkaDump();
        cmd.yaml = yaml;
        return cmd;
    }

    @Option(names = {"-y", "-yaml"}, defaultValue = "dump.yml")
    private File yaml;

    @Override
    public File getYamlFile() {
        if (!yaml.exists()) {
            throw new IllegalStateException("missing file with definition of kafka-dump.yml");
        }
        return yaml;
    }

    @Override
    public void run() {
        var config = parseYaml(Dump.class);
        var path = yaml.toPath().toAbsolutePath();
        var root = path.getParent();

        log.info("Start dumping from Kafka");
        new KafkaWorker(config, root).pool();
    }

    @Data
    @RequiredArgsConstructor
    @ToString
    public static class TopicContext {

        @ToString.Include()
        final String name;
        Long lastOffset = 0L;
        Range range;
        Descriptor descriptor;
        Output output;
        RecordWriter recordWriter;

        public boolean hasRecordsToCollect() {
            return nonNull(range) && !range.reachEnd(lastOffset);
        }

    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class KafkaWorker implements ProtoJsonWriter {

        final Dump mainConfig;
        final Path root;
        long recordCounter;
        int poolSize;
        Decoder protoDecoder;
        Map<String, TopicContext> contexts = new HashMap<>();

        @Override
        public String toJson(String topic, byte[] data) {
            return protoDecoder.toJson(contexts.get(topic).getDescriptor(), data);
        }

        private void pool() {
            var topics = mainConfig.getDump().getTopics()
                  .stream()
                  .map(t -> t.getName())
                  .collect(Collectors.toList());
            final var timeout = Duration.ofSeconds(mainConfig.getDump().getPoolTimeoutSec());
            initialize();

            try (KafkaConsumer<String, byte[]> consumer = kafka.getConsumerFactory().createConsumer(mainConfig)) {
                consumer.subscribe(topics);
                do {

                    var records = consumer.poll(timeout);
                    poolSize = records.count();
                    log.debug("Kafka pool size: {}", poolSize);

                    records.forEach(this::dumpRecord);
                } while (notEnds());
            }
            log.info("Stop dumping from Kafka, saved records: {}", recordCounter);
        }

        private void initialize() {
            var proto = initProto();
            var ranges = initRange();

            for (var topic : mainConfig.getDump().getTopics()) {
                var name = topic.getName();
                var context = contexts.computeIfAbsent(name, TopicContext::new);
                context.setRecordWriter(new RecordWriter(topic, this));

                var output = outputFactory.getCreator().apply(root, topic.getOutput());
                output.open();
                context.setOutput(output);
                context.setRange(ranges.get(name));
                context.setDescriptor(proto.get(name));
            }
        }


        private Map<String, Range> initRange() {
            return mainConfig.getDump().getTopics()
                  .stream()
                  .filter(t -> hasAnyValue(t.getRange()))
                  .collect(Collectors.toConcurrentMap(Topic::getName, topic -> {
                      var range = new Range(topic.getRange());
                      log.info("Topic {} with range: {}", topic.getName(), range);
                      return range;
                  }));
        }

        private Map<String, Descriptor> initProto() {
            var protoSpec = mainConfig.getDump().getProto();
            if (nonNull(protoSpec)) {
                protoDecoder = new Decoder();
                protoDecoder.loadDescriptors(protoSpec);

                return mainConfig.getDump()
                      .getTopics()
                      .stream()
                      .filter(t -> hasAnyValue(t.getValue().getProtoMessage()))
                      .collect(Collectors.toMap(
                            Topic::getName,
                            t -> protoDecoder.findMessageDescriptor(t.getValue().getProtoMessage())
                      ));
            }
            return Collections.emptyMap();
        }

        private boolean notEnds() {
            var collectingTopics = contexts.values()
                  .stream()
                  .filter(TopicContext::hasRecordsToCollect)
                  .count();
            return collectingTopics > 0;
        }

        private void dumpRecord(ConsumerRecord<String, byte[]> record) {
            var ctx = contexts.get(record.topic());
            var lastOffset = record.offset();
            ctx.setLastOffset(lastOffset);

            var range = ctx.getRange();
            if (nonNull(range) && range.positionNotInRange(lastOffset)) {
                return;
            }
            var txt = ctx.getRecordWriter().toJsonString(record);

            ctx.getOutput().emit(String.valueOf(lastOffset), txt.getBytes(StandardCharsets.UTF_8));

            recordCounter++;
        }
    }
}
