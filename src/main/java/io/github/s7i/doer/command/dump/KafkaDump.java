package io.github.s7i.doer.command.dump;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.protobuf.Descriptors.Descriptor;
import io.github.s7i.doer.command.YamlParser;
import io.github.s7i.doer.config.Dump;
import io.github.s7i.doer.config.Dump.Specs;
import io.github.s7i.doer.config.Range;
import io.github.s7i.doer.proto.Decoder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kdump")
@Slf4j
public class KafkaDump implements Runnable, YamlParser {

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

        var destPath = root.resolve(config.getDump().getOutput());
        try {
            Files.createDirectories(destPath);
        } catch (IOException e) {
            log.error("{}", e);
        }
        new KafkaWorker(config, root).pool();
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class KafkaWorker implements ProtoJsonWriter {

        final Dump mainConfig;
        final Path root;
        long lastOffset;
        Range range;
        int poolSize;
        Decoder protoDecoder;
        Descriptor descriptor;
        RecordWriter recordWriter;

        @Override
        public String toJson(byte[] data) {
            return protoDecoder.toJson(descriptor, data);
        }

        private void pool() {
            final var specs = mainConfig.getDump();
            initProto(specs);
            initRange(specs);
            recordWriter = new RecordWriter(specs, this);

            Properties properties = new Properties();
            properties.putAll(mainConfig.getKafka());

            try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer(properties)) {
                var topic = specs.getTopic();
                consumer.subscribe(List.of(topic));
                do {
                    var timeout = Duration.ofSeconds(specs.getPoolTimeoutSec());
                    var records = consumer.poll(timeout);
                    poolSize = records.count();
                    records.forEach(this::dumpRecord);
                } while (notEnds());
            }
        }

        private void initRange(Specs specs) {
            var rangeExp = specs.getRange();
            if (nonNull(rangeExp) && !rangeExp.isBlank()) {
                range = new Range(rangeExp);
                log.info("range: {}", range);
            }
        }

        private void initProto(Specs specs) {
            if (specs.isShowProto()) {
                protoDecoder = new Decoder();
                var descriptorPaths = Arrays.stream(specs.getProto().getDescriptorSet())
                      .map(Paths::get)
                      .collect(Collectors.toList());
                protoDecoder.loadDescriptors(descriptorPaths);

                descriptor = protoDecoder.findMessageDescriptor(specs.getProto().getMessageName());
            }
        }

        private boolean notEnds() {
            if (nonNull(range) && range.outOfTo(lastOffset)) {
                return false;
            } else if (isNull(range)) {
                return poolSize > 0;
            }
            return true;
        }

        private void dumpRecord(ConsumerRecord<String, byte[]> record) {
            lastOffset = record.offset();
            if (nonNull(range) && range.in(lastOffset)) {
                return;
            }

            final var fileName = lastOffset + ".txt";
            final var specs = mainConfig.getDump();
            final var location = root.resolve(specs.getOutput()).resolve(fileName);

            if (Files.exists(location)) {
                return;
            }

            try {
                var text = recordWriter.toJsonString(record);
                Files.writeString(location, text, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (IOException | RuntimeException e) {
                log.error("{}", e);
            }
        }
    }
}
