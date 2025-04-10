package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.meshtastic.MeshWebClient;
import io.vertx.core.Verticle;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Slf4j
@Command(
      name = "meshradio",
      description = "Meshtastic radio support."
)
public class Meshtastic extends VerticleCommand {

    @Slf4j
    @Accessors(fluent = true)
    @Getter
    public static class Options implements KafkaConfig {

        @Option(names = "-p", description = "port", defaultValue = "80")
        int port;
        @Option(names = {"-h", "--host"}, description = "Radio IP", required = true)
        String host;
        @Option(names = "--interval", description = "API call interval. (milliseconds)", defaultValue = "5000")
        int interval;
        @Option(names = "--kafka-config", description = "Kafka properties file.")
        String kafkaConfig;
        @Option(names = "--kafka-topic", defaultValue = "meshtastic-from-radio")
        String kafkaTopic;
        @Option(names = "--ask-for-config", description = "Send at begin request for config.")
        boolean askForConfig;
        @Option(names = "-q", description = "Quiet, less verbose.")
        boolean quiet;
        @Option(names = "--commit-timeout", description = "Kafka Commit Timeout second.", defaultValue = "30")
        int kafkaAsyncCommitTimeout;

        @Override
        public String getKafkaPropFile() {
            log.debug("using config file: {}", kafkaConfig);
            return requireNonNull(kafkaConfig);
        }

        @Override
        public Map<String, String> getKafka() {
            return Map.of();
        }
    }

    @Mixin
    private Options option;

    @Override
    protected Verticle createVerticle() {
        return new MeshWebClient(requireNonNull(option));
    }

}
