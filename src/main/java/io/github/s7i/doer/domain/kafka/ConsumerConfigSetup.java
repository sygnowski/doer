package io.github.s7i.doer.domain.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;

public interface ConsumerConfigSetup extends KafkaConfig {

    default void disableAutoCommit() {
        configure(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(false));
    }
    default void configureMaxPool(int maxPoolRecords) {
        configure(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPoolRecords));
    }
    default void configure(String option, String value) {
        getKafka().put(option, value);
    }

}
