package io.github.s7i.doer.domain.kafka.output;

import static java.util.Objects.nonNull;

import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;

@Builder
public class KafkaOutput implements Output {

    final KafkaFactory kafkaFactory;
    final KafkaConfig config;
    final boolean userTracing;
    final TopicPartition topic;
    Producer<String, byte[]> producer;

    @Override
    public void open() {
        producer = kafkaFactory.getProducerFactory().createProducer(config, userTracing);
    }

    @Override
    public void emit(Load load) {
        var record = new ProducerRecord<>(topic.topic(), load.getKey(), load.getData());
        if (nonNull(load.getMeta())) {
            load.getMeta().forEach((k, v) -> record.headers().add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8))));
        }
        producer.send(record);
    }

    @Override
    public void close() throws Exception {
        if (nonNull(producer)) {
            producer.close();
        }
    }
}
