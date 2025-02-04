package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import com.geeksville.mesh.MeshProtos.FromRadio;
import com.google.protobuf.InvalidProtocolBufferException;
import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.github.s7i.meshtastic.Proto;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
      name = "meshradio",
      description = "Meshtastic radio support."
)
public class Meshtastic extends VerticleCommand {

    @Slf4j
    public static class Options implements KafkaConfig {

        @Option(names = "-p", description = "port", defaultValue = "80")
        int port;
        @Option(names = {"-h", "--host"}, description = "Radio IP", required = true)
        String host;
        @Option(names = "--interval", description = "API call interval.", defaultValue = "5")
        int interval;
        @Option(names = "--kafka-config", description = "Kafka properties file.")
        String kafkaConfig;
        @Option(names = "--kafka-topic", defaultValue = "meshtastic-from-radio")
        String kafkaTopic;
        @Option(names = "--ask-for-config", description = "Send at begin request for config.")
        boolean askForConfig;
        @Option(names = "-q", description = "Quiet, less verbose.")
        boolean quiet;

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

    @RequiredArgsConstructor
    @Slf4j
    public static class MeshWebClient extends AbstractVerticle implements ConsoleLog {

        public static final String API_FROM_RADIO = "/api/v1/fromradio?all=false";
        public static final String API_TO_RADIO = "/api/v1/toradio";
        private final Options options;
        private WebClient client;
        private WorkerExecutor executor;
        private Producer<String, byte[]> producer;

        @Override
        public void init(Vertx vertx, Context context) {
            super.init(vertx, context);

            var clientOptions = new WebClientOptions();
            client = WebClient.create(vertx, clientOptions);
            executor = vertx.createSharedWorkerExecutor("async-op");
            if (options.kafkaConfig != null) {
                producer = initKafkaProducer();
            }
        }

        @Override
        public void start() throws Exception {
            if (options.askForConfig) {

                var configId = new Random().nextInt();
                client.put(options.port, options.host, API_TO_RADIO)
                      .sendBuffer(Buffer.buffer(Proto.INSTANCE.getConfiguration(configId).toByteArray()))
                      .onSuccess(this::extractBody)
                      .onFailure(expect -> log.error("oops", expect));
            }

            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(options.interval), this::callRadio);
        }

        private void extractBody(HttpResponse<Buffer> resp) {
            var body = resp.body();
            if (body != null) {
                publish(body.getBytes());
            }
        }

        void callRadio(Long t) {
            var req = client.get(options.port, options.host, API_FROM_RADIO);
            req.send(rep -> {
                if (rep.succeeded()) {
                    var result = rep.result();
                    if (result.statusCode() == 200) {
                        extractBody(result);
                    }
                } else {
                    log.error("oops", rep.cause());
                }
            });
        }

        private void verbose(Runnable stmt) {
            if (!options.quiet) {
                stmt.run();
            }
        }

        private void publish(byte[] payload) {
            verbose(() -> info("---\n{}\n---", Base64.getEncoder().encodeToString(payload)));
            try {
                var fromRadio = FromRadio.parseFrom(payload);

                verbose(() -> info("Proto: \n{}", fromRadio));

                boolean skip = switch (fromRadio.getPayloadVariantCase()) {
                    case QUEUESTATUS, CONFIG_COMPLETE_ID -> true;
                    default -> false;
                };
                if (options.kafkaConfig != null && !skip) {
                    publishToKafka(payload).onSuccess(meta -> log.info("payload published: {}", meta));
                }
            } catch (InvalidProtocolBufferException bpe) {
                log.warn("cannot parse proto: {}", payload);
            }
        }

        Future<RecordMetadata> publishToKafka(byte[] data) {
            return executor.executeBlocking(() -> producer.send(new ProducerRecord<>(options.kafkaTopic, data)).get());
        }

        private Producer<String, byte[]> initKafkaProducer() {
            return new KafkaFactory()
                  .getProducerFactory()
                  .createProducer(options, false);
        }
    }
}
