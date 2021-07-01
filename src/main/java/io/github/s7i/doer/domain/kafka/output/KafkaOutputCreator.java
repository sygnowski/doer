package io.github.s7i.doer.domain.kafka.output;

import io.github.s7i.doer.config.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.doer.domain.output.Output;
import io.github.s7i.doer.domain.output.creator.OutputCreator;

public interface KafkaOutputCreator extends OutputCreator {

    default Output create() {
        return new KafkaOutput(getKafkaFactory(), getKafkaConfig(), getUseTracing());
    }

    KafkaConfig getKafkaConfig();

    boolean getUseTracing();

    KafkaFactory getKafkaFactory();
}
