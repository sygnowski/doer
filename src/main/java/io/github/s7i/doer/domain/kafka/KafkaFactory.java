package io.github.s7i.doer.domain.kafka;

import io.github.s7i.doer.Tracing;
import io.opentracing.contrib.kafka.TracingKafkaConsumer;
import io.opentracing.contrib.kafka.TracingKafkaProducer;
import java.util.Properties;
import lombok.Getter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

public class KafkaFactory {

    public KafkaFactory() {
        this(KafkaFactory::createProducer, KafkaFactory::createConsumer);
    }

    public KafkaFactory(KafkaProducerFactory producerFactory, KafkaConsumerFactory consumerFactory) {
        this.producerFactory = producerFactory;
        this.consumerFactory = consumerFactory;
    }

    @Getter
    final KafkaProducerFactory producerFactory;

    @Getter
    final KafkaConsumerFactory consumerFactory;

    static Producer<String, byte[]> createProducer(KafkaConfig config, boolean useTracing) {
        var props = getProperties(config);
        var producer = new KafkaProducer<String, byte[]>(props);
        if (useTracing) {
            return new TracingKafkaProducer<>(producer, Tracing.INSTANCE.getTracer());
        }
        return producer;

    }

    static Consumer<String, byte[]> createConsumer(KafkaConfig config, boolean useTracing) {
        var properties = getProperties(config);
        final var consumer = new KafkaConsumer<String, byte[]>(properties);
        if (useTracing) {
            return new TracingKafkaConsumer<>(consumer, Tracing.INSTANCE.getTracer());
        }
        return consumer;
    }

    static Properties getProperties(KafkaConfig config) {
        return new KafkaPropertiesReader(config)
              .getProperties();
    }
}
