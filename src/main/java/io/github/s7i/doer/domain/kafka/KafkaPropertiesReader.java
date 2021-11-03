package io.github.s7i.doer.domain.kafka;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaPropertiesReader {

    @Getter
    private final Properties properties;

    public KafkaPropertiesReader(KafkaConfig config) {
        properties = new Properties();
        readPropFile(config);
        assignProps(config);
    }

    private void assignProps(KafkaConfig config) {
        if (nonNull(config.getKafka())) {
            properties.putAll(config.getKafka());
        }
    }

    private void readPropFile(KafkaConfig config) {
        var propFile = config.getKafkaPropFile();
        if (nonNull(propFile) && !propFile.isBlank()) {
            try (var reader = Files.newBufferedReader(Path.of(propFile))) {
                properties.load(reader);
            } catch (IOException e) {
                log.error("reading kafka properties file", e);
            }
        }
    }


}
