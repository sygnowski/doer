package io.github.s7i.doer.command;

import static java.util.Objects.requireNonNull;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import io.github.s7i.doer.ConsoleLog;
import io.github.s7i.doer.domain.kafka.KafkaConfig;
import io.github.s7i.doer.domain.kafka.KafkaFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "meshradio")
public class Meshtastic extends VerticleCommand {

    @Slf4j
    public static class Options implements KafkaConfig {

        @Option(names = "-p", description = "port", defaultValue = "80")
        int port;
        @Option(names = "-h", description = "Radio IP", required = true)
        String host;
        @Option(names = "--interval", description = "Api call interval.", defaultValue = "5")
        int interval;
        @Option(names = "--kafka-config", description = "Kafka properties file.")
        String kafkaConfig;
        @Option(names = "--kafka-topic", defaultValue = "meshtastic-from-radio")
        String kafkaTopic;

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
            vertx.setPeriodic(TimeUnit.SECONDS.toMillis(options.interval), this::callRadio);
        }

        void callRadio(Long t) {
            var req = client.get(options.port, options.host, API_FROM_RADIO);
            req.send(rep -> {
                if (rep.succeeded()) {
                    var result = rep.result();
                    if (result.statusCode() == 200) {
                        var body = result.body();
                        if (body == null) {
                            return;
                        }
                        var payload = body.getBytes();
                        info("---\n{}\n---", Base64.getEncoder().encodeToString(payload));

                        if (options.kafkaConfig != null) {
                            publishToKafka(payload).onSuccess(meta -> log.info("payload published: {}", meta));
                        }

                        try {
                            info("Proto: \n{}", UnknownFieldSet.parseFrom(payload));
                        } catch (InvalidProtocolBufferException bpe) {
                            log.warn("cannot parse proto: {}", payload);
                        }
                    }
                } else {
                    log.error("oops", rep.cause());
                }
            });
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
