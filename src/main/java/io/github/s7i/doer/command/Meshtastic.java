package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import io.github.s7i.doer.Context;
import io.github.s7i.doer.command.Meshtastic.WebRadioApi;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.meshtastic.MeshWebClient;
import io.github.s7i.doer.domain.output.DefaultOutputProvider;
import io.github.s7i.doer.domain.output.Output.Load;
import io.github.s7i.meshtastic.Constrains;
import io.github.s7i.meshtastic.Proto;
import io.vertx.core.Verticle;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Slf4j
@Command(
      name = "mesh",
      description = "//\\ - Meshtastic",
      subcommands = WebRadioApi.class
)
public class Meshtastic implements DefaultOutputProvider {


    @Command(name = "txt")
    public void textMessage(
          @Option(names = "-m", required = true) String text,
          @Option(names = "-o")
          boolean userOutput
    ) {
        Supplier<Integer> from = () -> (int) (4072996141L & 0xffffffffL);
        Supplier<Integer> to = () -> Constrains.NODENUM_BROADCAST;
        var msg = Proto.INSTANCE.textMessage(from, to, text);
        System.out.println(msg);

        if (userOutput) {
            var context = new Context.Initializer(Context.InitialParameters.builder()
                  .workDir(Path.of("."))
                  .build())
                  .context();

            getDefaultOutput(context).emit(Load.builder()
                  .key("192.168.0.170")
                  .data(msg.toByteArray())
                  .build());
        }
    }


    @Command(name = "web-api")
    public static class WebRadioApi extends VerticleCommand {

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


}
