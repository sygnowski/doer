package io.github.s7i.doer.domain.kafka;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

public class ConsumerConfigFile implements ConsumerConfigSetup {

    private final String kafkaConfigPath;
    private final Map<String, String> kafkaConfig = new HashMap<>();

    public ConsumerConfigFile(String kafkaConfigPath) {
        this.kafkaConfigPath = requireNonNull(kafkaConfigPath);

    }

    @Override
    public String getKafkaPropFile() {
        return kafkaConfigPath;
    }

    @Override
    public Map<String, String> getKafka() {
        return kafkaConfig;
    }
}
