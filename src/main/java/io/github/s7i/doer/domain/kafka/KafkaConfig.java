package io.github.s7i.doer.domain.kafka;

import java.util.Map;

public interface KafkaConfig {

    default String getKafkaPropFile() {
        return null;
    }

    Map<String, String> getKafka();
}
