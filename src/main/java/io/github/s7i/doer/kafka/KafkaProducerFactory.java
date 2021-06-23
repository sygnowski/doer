package io.github.s7i.doer.kafka;

import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;

public interface KafkaProducerFactory {

    Producer<String, byte[]> build(Properties prop, boolean useTracing);
}
