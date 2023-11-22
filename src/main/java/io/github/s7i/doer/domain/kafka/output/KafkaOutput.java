package io.github.s7i.doer.domain.kafka.output;

import io.github.s7i.doer.DoerException;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Builder
@Slf4j
public class KafkaOutput implements Output {

    public static final int RECORD_TIMEOUT_SEC = 30;
    final KafkaFactory kafkaFactory;
    final KafkaConfig config;
    final boolean userTracing;
    final TopicPartition topic;
    Producer<String, byte[]> producer;

    boolean closed;

    @Override
    public void open() {
        if (nonNull(producer)) {
            return;
        }
        producer = kafkaFactory.getProducerFactory().createProducer(config, userTracing);
    }

    @Override
    public void emit(Load load) {
        if (closed) {
            throw new DoerException("closed");
        }
        requireNonNull(producer, "producer");
        var record = new ProducerRecord<>(topic.topic(), load.getKey(), load.getData());
        if (nonNull(load.getMeta())) {
            load.getMeta().forEach((k, v) -> record.headers().add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8))));
        }
        log.debug("sending record...");
        try {
            producer.send(record, (m, t) -> log.debug("rec-callback: {}", m, t))
                    .get(RECORD_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException itr) {
            Thread.currentThread().interrupt();
            throw new DoerException("interrupted thread");
        } catch (ExecutionException | TimeoutException e) {
            throw new DoerException(e);
        }
    }

    @Override
    public void close() throws Exception {
        if (nonNull(producer)) {
            producer.close();
        }
        closed = true;
        log.debug("closed");
    }
}
