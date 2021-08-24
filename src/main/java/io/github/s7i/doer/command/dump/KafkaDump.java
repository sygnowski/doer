package io.github.s7i.doer.command.dump;


import static io.github.s7i.doer.util.Utils.hasAnyValue;
import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.config.Dump;
import io.github.s7i.doer.config.Dump.Topic;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kdump")
@Slf4j
public class KafkaDump implements Runnable, YamlParser {

    public static KafkaFactory kafka = new KafkaFactory();

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
        Path output;
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
            initialize();

            var topics = mainConfig.getDump().getTopics()
                  .stream()
                  .map(t -> t.getName())
                  .collect(Collectors.toList());
            final var timeout = Duration.ofSeconds(mainConfig.getDump().getPoolTimeoutSec());

            try (KafkaConsumer<String, byte[]> consumer = kafka.getConsumerFactory().createConsumer(mainConfig)) {

                consumer.subscribe(topics, new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

                    }

                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        consumer.committed(new HashSet<>(partitions)).forEach((tp, offset) -> log.info("current offset {} for {}", tp, offset));
                        for (var tp : partitions) {
                            var ctx = contexts.get(tp.topic());
                            if (nonNull(ctx) && nonNull(ctx.getRange()) && ctx.getRange().hasFrom()) {
                                var offset = ctx.getRange().getFrom();
                                log.info("seeking to offset {} on partition {}", offset, tp);
                                consumer.seek(tp, offset);
                            }
                        }
                    }
                });

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
                var topicOutput = root.resolve(topic.getOutput());
                try {
                    Files.createDirectories(topicOutput);
                } catch (IOException e) {
                    log.error("{}", e);
                }
                context.setOutput(topicOutput);
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

            final var fileName = lastOffset + ".txt";
            final var location = ctx.getOutput().resolve(fileName);

            if (!Files.exists(location)) {
                try {
                    var text = ctx.getRecordWriter().toJsonString(record);
                    log.debug("write to file: {}", location);
                    Files.writeString(location, text, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                    recordCounter++;
                } catch (IOException | RuntimeException e) {
                    log.error("{}", e);
                }
            }
        }
    }
}
