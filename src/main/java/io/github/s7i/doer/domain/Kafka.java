package io.github.s7i.doer.domain;

import io.github.s7i.doer.config.KafkaConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;


public class Kafka {

    public Producer<String, byte[]> createProducer(KafkaConfig config) {
        var properties = new KafkaPropertiesReader(config)
              .getProperties();
        return new KafkaProducer<>(properties);

    }

    public KafkaConsumer<String, byte[]> createConsumer(KafkaConfig config) {
        var properties = new KafkaPropertiesReader(config)
              .getProperties();
        return new KafkaConsumer<>(properties);
    }
}
