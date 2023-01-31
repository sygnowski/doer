package io.github.s7i.doer.domain.kafka;

import io.github.s7i.doer.util.PathResolver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static java.util.Objects.nonNull;

@Slf4j
public class KafkaPropertiesReader implements PathResolver {

    @Getter
    private final Properties properties;

    public KafkaPropertiesReader(KafkaConfig config) {
        properties = new Properties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

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
            try (var reader = Files.newBufferedReader(resolvePath(propFile))) {
                properties.load(reader);
            } catch (IOException e) {
                log.error("reading kafka properties file", e);
            }
        }
    }


}
