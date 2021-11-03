package io.github.s7i.doer.domain.kafka.output;


import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.creator.OutputCreator;
import org.apache.kafka.common.TopicPartition;

public interface KafkaOutputCreator extends OutputCreator {

    default Output create() {
        return KafkaOutput.builder()
              .config(getKafkaConfig())
              .userTracing(getUseTracing())
              .kafkaFactory(getKafkaFactory())
              .topic(getTopic())
              .build();
    }

    KafkaConfig getKafkaConfig();

    boolean getUseTracing();

    KafkaFactory getKafkaFactory();

    TopicPartition getTopic();
}
