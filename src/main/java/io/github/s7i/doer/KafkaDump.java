package io.github.s7i.doer;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
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
    private static class KafkaWorker {

        public static final String NEWLINE = "\n";
        final Dump config;
        final Path root;
        long lastOffset;
        Range range;
        int poolSize;

        private void pool() {

            var rangeExp = config.getDump().getRange();
            if (nonNull(rangeExp) && !rangeExp.isBlank()) {
                range = new Range(rangeExp);
                log.info("range: {}", range);
            }

            Properties properties = new Properties();
            properties.putAll(config.getKafka());

            try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer(properties)) {
                var topic = config.getDump().getTopic();
                consumer.subscribe(List.of(topic));
                do {
                    var timeout = Duration.ofSeconds(config.getDump().getPoolTimeoutSec());
                    var records = consumer.poll(timeout);
                    poolSize = records.count();
                    records.forEach(this::dumpRecord);
                } while (notEnds());
            }
        }

        private boolean notEnds() {
            if (nonNull(range) && range.hasTo() && lastOffset >= range.getTo()) {
                return false;
            } else if (isNull(range)) {
                return poolSize > 0;
            }
            return true;
        }

        private void dumpRecord(ConsumerRecord<String, byte[]> record) {
            lastOffset = record.offset();
            if (nonNull(range)) {
                if (range.hasFrom() && lastOffset < range.getFrom()) {
                    return;
                }
                if (range.hasTo() && lastOffset > range.getTo()) {
                    return;
                }
            }

            String fileName = lastOffset + ".txt";
            final var dumpCfg = config.getDump();
            final var location = root.resolve(dumpCfg.getOutput()).resolve(fileName);

            if (Files.exists(location)) {
                return;
            }

            StringBuilder text = new StringBuilder();
            try {
                writeRecordToText(record, dumpCfg, text);
                Files.writeString(location, text.toString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (IOException | RuntimeException e) {
                log.error("{}", e);
            }
        }

        private void writeRecordToText(ConsumerRecord<String, byte[]> record, Dump.DumpEntry dumpCfg, StringBuilder text) {
            text.append("KEY: ").append(record.key()).append(NEWLINE);
            text.append("TIMESTAMP: ")
                  .append(Instant.ofEpochMilli(record.timestamp()).toString())
                  .append(NEWLINE);

            text.append("HEADERS:").append(NEWLINE);
            for (var header : record.headers()) {
                text.append(header.key())
                      .append(": ")
                      .append(new String(header.value()))
                      .append(NEWLINE);
            }
            text.append(NEWLINE);

            var bytes = record.value();

            if (dumpCfg.isShowBinary()) {
                text.append("BINARY_BEGIN").append(NEWLINE);
                text.append(new String(bytes));
                text.append(NEWLINE).append("BINARY_END").append(NEWLINE);
            }

            if (dumpCfg.isShowProto()) {
                text.append("PROTO_AS_JSON:").append(NEWLINE);
                var descriptorPaths = Arrays.stream(dumpCfg.getProto().getDescriptorSet())
                      .map(Paths::get)
                      .collect(Collectors.toList());

                var json = new ProtoProcessor().toJson(descriptorPaths, dumpCfg.getProto().getMessageName(), bytes);
                text.append(json);
            }
        }
    }
}
