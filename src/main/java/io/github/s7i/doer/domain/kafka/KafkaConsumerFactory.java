package io.github.s7i.doer.domain.kafka;

import io.github.s7i.doer.config.KafkaConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface KafkaConsumerFactory {

    KafkaConsumer<String, byte[]> createConsumer(KafkaConfig config);
}
