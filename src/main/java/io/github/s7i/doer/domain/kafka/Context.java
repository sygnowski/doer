package io.github.s7i.doer.domain.kafka;

import io.github.s7i.doer.Globals;

public interface Context extends io.github.s7i.doer.Context {

    default KafkaFactory getKafkaFactory() {
        return Globals.INSTANCE.getKafka();
    }

}
