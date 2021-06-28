package io.github.s7i.doer.domain.kafka;

import io.github.s7i.doer.config.KafkaConfig;
import org.apache.kafka.clients.producer.Producer;

public interface KafkaProducerFactory {

    Producer<String, byte[]> createProducer(KafkaConfig config, boolean useTracing);
}
