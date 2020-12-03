package io.github.s7i.doer;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "kdump")
@Slf4j
public class KafkaDump implements Runnable {

    @Option(names = {"-y", "-yaml"})
    private File yaml;


    private Dump parseYaml() {
        var objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(yaml, Dump.class);
        } catch (IOException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        var config = parseYaml();
        var root = yaml.toPath().getParent();

        var destPath = root.resolve(config.getDump().getOutput());
        try {
            Files.createDirectories(destPath);
        } catch (IOException e) {
            log.error("{}", e);
        }
        new KafkaWorker(config, root).pool();
    }

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    private static class KafkaWorker {

        public static final String NEWLINE = "\n";
        Dump config;
        Path root;

        private void pool() {

            Properties properties = new Properties();
            properties.putAll(config.getKafka());

            try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer(properties)) {
                var topic = config.getDump().getTopic();
                consumer.subscribe(List.of(topic));

                int poolSize;
                do {
                    var timeout = Duration.ofSeconds(config.getDump().getPoolTimeoutSec());
                    var records = consumer.poll(timeout);
                    poolSize = records.count();
                    records.forEach(this::dumpRecord);
                } while (poolSize > 0);
            }
        }

        private void dumpRecord(ConsumerRecord<String, byte[]> record) {
            String fileName = record.offset() + ".txt";
            StringBuilder text = new StringBuilder();
            try {
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
                var dumpCfg = config.getDump();
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

                var location = root.resolve(dumpCfg.getOutput()).resolve(fileName);
                Files.writeString(location, text.toString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (IOException | RuntimeException e) {
                log.error("{}", e);
            }
        }
    }
}
